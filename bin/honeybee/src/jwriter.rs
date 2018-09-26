/*
 * Copyright (C) 2018  Minnesota Department of Transportation
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
use failure::Error;
use fallible_iterator::FallibleIterator;
use postgres::{Connection, TlsMode};
use std::fs::File;
use std::io::{BufWriter,Write};

static CAMERA_SQL: &str = "SELECT row_to_json(r)::text FROM (\
    SELECT name, publish, location, lat, lon \
    FROM camera_view \
    ORDER BY name \
) r";

static DMS_SQL: &str = "SELECT row_to_json(r)::text FROM (\
    SELECT name, sign_config, roadway, road_dir, cross_street, \
           location, lat, lon \
    FROM dms_view \
    ORDER BY name \
) r";

static DMS_MSG_SQL: &str = "SELECT row_to_json(r)::text FROM (\
    SELECT name, msg_current, multi, sources, duration, expire_time \
    FROM dms_message_view WHERE condition = 'Active' \
    ORDER BY name \
) r";

static INCIDENT_SQL: &str = "SELECT row_to_json(r)::text FROM (\
    SELECT name, event_date, description, road, direction, lane_type, \
           impact, confirmed, camera, detail, replaces, lat, lon \
    FROM incident_view \
    WHERE cleared = false \
) r";

static SIGN_CONFIG_SQL: &str = "SELECT row_to_json(r)::text FROM (\
    SELECT name, dms_type, portable, technology, sign_access, legend, \
           beacon_type, face_width, face_height, border_horiz, \
           border_vert, pitch_horiz, pitch_vert, pixel_width, \
           pixel_height, char_width, char_height, color_scheme, \
           monochrome_foreground, monochrome_background \
    FROM sign_config_view \
) r";

static TPIMS_STAT_SQL: &str = "SELECT row_to_json(r)::text FROM (\
    SELECT site_id AS \"siteId\", to_char(time_stamp_static, \
           'YYYY-mm-dd\"T\"HH24:MI:SSZ') AS \"timeStamp\", \
           relevant_highway AS \"relevantHighway\", \
           reference_post AS \"referencePost\", exit_id AS \"exitId\", \
           road_dir AS \"directionOfTravel\", facility_name AS name, \
           json_build_object('latitude', lat, 'longitude', lon, \
           'streetAdr', street_adr, 'city', city, 'state', state, \
           'zip', zip, 'timeZone', time_zone) AS location, \
           ownership, capacity, \
           string_to_array(amenities, ', ') AS amenities, \
           array_remove(ARRAY[camera_image_base_url || camera_1, \
           camera_image_base_url || camera_2, \
           camera_image_base_url || camera_3], NULL) AS images, \
           ARRAY[]::text[] AS logos \
    FROM parking_area_view \
) r";

static TPIMS_DYN_SQL: &str = "SELECT row_to_json(r)::text FROM (\
    SELECT site_id AS \"siteId\", to_char(time_stamp, \
           'YYYY-mm-dd\"T\"HH24:MI:SSZ') AS \"timeStamp\", \
           to_char(time_stamp_static, 'YYYY-mm-dd\"T\"HH24:MI:SSZ') \
           AS \"timeStampStatic\", \
           reported_available AS \"reportedAvailable\", \
           trend, open, trust_data AS \"trustData\", capacity \
    FROM parking_area_view \
) r";

struct Request {
    sql: &'static str,
    file_name: &'static str,
}

impl Request {
    fn new(sql: &'static str, file_name: &'static str) -> Self {
        Request { sql, file_name }
    }
}

fn request(n: &str) -> Option<Request> {
    match n {
        "camera"        => Some(Request::new(CAMERA_SQL, "camera.json")),
        "dms"           => Some(Request::new(DMS_SQL, "dms.json")),
        "dms_message"   => Some(Request::new(DMS_MSG_SQL, "dms_message.json")),
        "incident"      => Some(Request::new(INCIDENT_SQL, "incident.json")),
        "sign_config"   => Some(Request::new(SIGN_CONFIG_SQL,
                                "sign_config.json")),
        "TPIMS_static"  => Some(Request::new(TPIMS_STAT_SQL,
                                "TPIMS_static.json")),
        "TPIMS_dynamic" => Some(Request::new(TPIMS_DYN_SQL,
                                "TPIMS_dynamic.json")),
        _               => None,
    }
}

pub fn start(uds: String) -> Result<(), Error> {
    let conn = Connection::connect(uds, TlsMode::None)?;
    for r in ["camera", "dms", "dms_message", "incident", "sign_config",
              "TPIMS_static", "TPIMS_dynamic"].iter()
    {
        query_json_file(&conn, r)?;
    }
    notify_loop(&conn)
}

fn query_json_file(conn: &Connection, n: &str)
    -> Result<(), Error>
{
    let jd = request(n);
    if let Some(jd) = jd {
        let f = BufWriter::new(File::create(jd.file_name)?);
        let r = query_json(&conn, jd.sql, f)?;
        println!("wrote {} rows to {}", r, jd.file_name);
    } else {
        println!("unknown name {}", n);
    }
    Ok(())
}

fn query_json<T: Write>(conn: &Connection, q: &str, mut w: T)
    -> Result<u32, Error>
{
    let mut c = 0;
    w.write("[".as_bytes())?;
    for row in &conn.query(q, &[])? {
        if c > 0 { w.write(",".as_bytes())?; }
        w.write("\n".as_bytes())?;
        let j: String = row.get(0);
        w.write(j.as_bytes())?;
        c += 1;
    }
    if c > 0 { w.write("\n".as_bytes())?; }
    w.write("]\n".as_bytes())?;
    Ok(c)
}

fn notify_loop(conn: &Connection) -> Result<(), Error> {
    &conn.execute("LISTEN tms", &[])?;
    let nots = conn.notifications();
    loop {
        for n in nots.blocking_iter().iterator() {
            let n = n?;
            query_json_file(&conn, n.payload.as_ref())?;
        }
    }
}