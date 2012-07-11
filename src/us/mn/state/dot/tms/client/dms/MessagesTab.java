/*
 * IRIS -- Intelligent Roadway Information System
 * Copyright (C) 2009-2012  Minnesota Department of Transportation
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
package us.mn.state.dot.tms.client.dms;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import us.mn.state.dot.sched.ActionJob;
import us.mn.state.dot.sched.ListSelectionJob;
import us.mn.state.dot.sonar.Checker;
import us.mn.state.dot.sonar.client.TypeCache;
import us.mn.state.dot.tms.BitmapGraphic;
import us.mn.state.dot.tms.DMS;
import us.mn.state.dot.tms.DMSHelper;
import us.mn.state.dot.tms.DmsSignGroup;
import us.mn.state.dot.tms.Font;
import us.mn.state.dot.tms.MultiString;
import us.mn.state.dot.tms.RasterBuilder;
import us.mn.state.dot.tms.RasterGraphic;
import us.mn.state.dot.tms.SignGroup;
import us.mn.state.dot.tms.SignText;
import us.mn.state.dot.tms.SystemAttrEnum;
import us.mn.state.dot.tms.client.Session;
import us.mn.state.dot.tms.client.toast.TmsForm;
import us.mn.state.dot.tms.client.toast.WrapperComboBoxModel;
import us.mn.state.dot.tms.client.widget.ZTable;
import us.mn.state.dot.tms.utils.I18N;

/**
 * MessagesTab is a GUI tab for displaying and editing sign messages on a
 * DMS properties form.
 *
 * @author Douglas Lau
 */
public class MessagesTab extends JPanel {

	/** Sign group table model */
	protected final SignGroupTableModel sign_group_model;

	/** Sign group table component */
	protected final ZTable group_table = new ZTable();

	/** Button to delete a sign group */
	protected final JButton delete_group = new JButton("Delete Group");

	/** Sign text table model */
	protected SignTextTableModel sign_text_model;

	/** Sign text table component */
	protected final ZTable sign_text_table = new ZTable();

	/** Button to delete sign text message */
	protected final JButton delete_text = new JButton("Delete Message");

	/** Sign pixel panel */
	protected final SignPixelPanel pixel_panel = new SignPixelPanel(true,
		new Color(0, 0, 0.4f));

	/** Default font combo box */
	protected final JComboBox font_cmb = new JComboBox();

	/** AWS allowed component */
	protected final JCheckBox awsAllowed = new JCheckBox(
		I18N.get("dms.aws.allowed"));

	/** AWS controlled component */
	protected final JCheckBox awsControlled = new JCheckBox(
		I18N.get("device.style.aws_controlled"));

	/** User session */
	protected final Session session;

	/** DMS cache */
	protected final DmsCache dms_cache;

	/** DMS proxy */
	protected final DMS proxy;

	/** Create a new messages tab */
	public MessagesTab(Session s, DMS sign) {
		super(new GridBagLayout());
		session = s;
		dms_cache = s.getSonarState().getDmsCache();
		proxy = sign;
		sign_group_model = new SignGroupTableModel(s, sign);
		sign_group_model.initialize();
		initGroupTable();
		initSignTextTable();
		createActions();
		font_cmb.setModel(new WrapperComboBoxModel(
			dms_cache.getFontModel()));
		initWidgets();
	}

	/** Dispose of the form */
	public void dispose() {
		sign_group_model.dispose();
		if(sign_text_model != null)
			sign_text_model.dispose();
	}

	/** Initialize the widgets on the tab */
	protected void initWidgets() {
		GridBagConstraints bag = new GridBagConstraints();
		bag.insets.top = TmsForm.VGAP;
		bag.insets.left = TmsForm.HGAP;
		bag.insets.right = TmsForm.HGAP;
		bag.insets.bottom = TmsForm.VGAP;
		bag.fill = GridBagConstraints.BOTH;
		bag.gridx = 0;
		bag.gridy = 0;
		bag.weightx = 0.25f;
		bag.weighty = 1;
		JScrollPane scroll = new JScrollPane(group_table);
		add(scroll, bag);
		scroll = new JScrollPane(sign_text_table);
		scroll.setPreferredSize(new Dimension(440, 0));
		bag.gridx = 1;
		bag.gridy = 0;
		bag.weightx = 1;
		add(scroll, bag);
		bag.fill = GridBagConstraints.NONE;
		bag.gridx = 0;
		bag.gridy = 1;
		bag.weightx = 0;
		bag.weighty = 0;
		delete_group.setEnabled(false);
		add(delete_group, bag);
		bag.gridx = 1;
		delete_text.setEnabled(false);
		add(delete_text, bag);
		bag.gridx = 0;
		bag.gridy = 2;
		bag.gridwidth = 2;
		bag.fill = GridBagConstraints.BOTH;
		bag.weightx = 0.1f;
		bag.weighty = 0.1f;
		add(createPreviewPanel(), bag);
		bag.gridx = 0;
		bag.gridy = 3;
		bag.gridwidth = 1;
		bag.fill = GridBagConstraints.NONE;
		bag.weightx = 0;
		bag.weighty = 0;
		bag.anchor = GridBagConstraints.EAST;
		add(new JLabel("Default Font"), bag);
		bag.gridx = 1;
		bag.anchor = GridBagConstraints.WEST;
		add(font_cmb, bag);
		if(SystemAttrEnum.DMS_AWS_ENABLE.getBoolean()) {
			bag.anchor = GridBagConstraints.CENTER;
			bag.gridy = 4;
			bag.gridx = 0;
			add(awsAllowed, bag);
			bag.gridx = 1;
			add(awsControlled, bag);
		}
	}

	/** Create a message preview panel */
	protected JPanel createPreviewPanel() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder(
			"Message Preview"));
		panel.add(pixel_panel, BorderLayout.CENTER);
		pixel_panel.setPreferredSize(new Dimension(390, 32));
		return panel;
	}

	/** Create actions for button widgets */
	protected void createActions() {
		new ActionJob(this, delete_group) {
			public void perform() {
				SignGroup group = getSelectedGroup();
				if(group != null)
					group.destroy();
			}
		};
		new ActionJob(this, delete_text) {
			public void perform() throws Exception {
				SignText sign_text = getSelectedSignText();
				if(sign_text != null)
					sign_text.destroy();
			}
		};
		new ActionJob(this, font_cmb) {
			public void perform() {
				proxy.setDefaultFont(
					(Font)font_cmb.getSelectedItem());
			}
		};
		new ActionJob(this, awsAllowed) {
			public void perform() {
				proxy.setAwsAllowed(awsAllowed.isSelected());
			}
		};
		new ActionJob(this, awsControlled) {
			public void perform() {
				proxy.setAwsControlled(
					awsControlled.isSelected());
			}
		};
	}

	/** Initialize the sign group table */
	protected void initGroupTable() {
		final ListSelectionModel s = group_table.getSelectionModel();
		s.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		new ListSelectionJob(this, s) {
			public void perform() {
				if(!event.getValueIsAdjusting())
					selectGroup();
			}
		};
		group_table.setAutoCreateColumnsFromModel(false);
		group_table.setColumnModel(
			sign_group_model.createColumnModel());
		group_table.setModel(sign_group_model);
		group_table.setVisibleRowCount(12);
	}

	/** Select a new sign group */
	protected void selectGroup() {
		SignGroup group = getSelectedGroup();
		if(group != null) {
			if(sign_text_model != null)
				sign_text_model.dispose();
			sign_text_model = new SignTextTableModel(session,
				group);
			sign_text_model.initialize();
			sign_text_table.setColumnModel(
				sign_text_model.createColumnModel());
			sign_text_table.setModel(sign_text_model);
			delete_group.setEnabled(isGroupDeletable(group));
		} else {
			sign_text_table.setModel(new DefaultTableModel());
			delete_group.setEnabled(false);
		}
	}

	/** Check if a sign group is deletable */
	protected boolean isGroupDeletable(SignGroup sg) {
		return hasNoReferences(sg) && canRemove(sg);
	}

	/** Check if a sign group has no references */
	protected boolean hasNoReferences(SignGroup group) {
		return !(hasMembers(group) || hasSignText(group));
	}

	/** Check if a sign group has any members */
	protected boolean hasMembers(final SignGroup group) {
		TypeCache<DmsSignGroup> dms_sign_groups =
			dms_cache.getDmsSignGroups();
		return null != dms_sign_groups.findObject(
			new Checker<DmsSignGroup>()
		{
			public boolean check(DmsSignGroup g) {
				return g.getSignGroup() == group;
			}
		});
	}

	/** Check if a sign group has any sign text messages */
	protected boolean hasSignText(final SignGroup group) {
		TypeCache<SignText> sign_text = dms_cache.getSignText();
		return null != sign_text.findObject(new Checker<SignText>() {
			public boolean check(SignText t) {
				return t.getSignGroup() == group;
			}
		});
	}

	/** Check if the user can remove the specified sign group */
	protected boolean canRemove(SignGroup sg) {
		return sign_group_model.canRemove(sg);
	}

	/** Get the selected sign group */
	protected SignGroup getSelectedGroup() {
		ListSelectionModel s = group_table.getSelectionModel();
		return sign_group_model.getProxy(s.getMinSelectionIndex());
	}

	/** Initialize the sign text table */
	protected void initSignTextTable() {
		final ListSelectionModel s =
			sign_text_table.getSelectionModel();
		s.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		new ListSelectionJob(this, s) {
			public void perform() {
				if(!event.getValueIsAdjusting())
					selectSignText();
			}
		};
		sign_text_table.setAutoCreateColumnsFromModel(false);
		sign_text_table.setVisibleRowCount(12);
	}

	/** Select a new sign text message */
	protected void selectSignText() {
		Integer w = proxy.getFaceWidth();
		Integer lh = getLineHeightPixels();
		Integer hp = proxy.getHorizontalPitch();
		Integer vp = proxy.getVerticalPitch();
		Integer hb = proxy.getHorizontalBorder();
		if(w != null && lh != null && hp != null && vp != null &&
		   hb != null)
		{
			int h = lh * vp;
			pixel_panel.setPhysicalDimensions(w, h, hb, 0, hp, vp);
		}
		Integer wp = proxy.getWidthPixels();
		Integer cw = proxy.getCharWidthPixels();
		if(wp != null && lh != null && cw != null)
			pixel_panel.setLogicalDimensions(wp, lh, cw, 0);
		pixel_panel.repaint();
		SignText st = getSelectedSignText();
		if(st != null)
			pixel_panel.setGraphic(renderMessage(st));
		else
			pixel_panel.setGraphic(null);
		delete_text.setEnabled(canRemove(st));
	}

	/** Check if the user can remove the specified sign text */
	protected boolean canRemove(SignText st) {
		SignTextTableModel stm = sign_text_model;
		return (stm != null) && (st != null) && stm.canRemove(st);
	}

	/** Get the line height of the sign */
	protected Integer getLineHeightPixels() {
		RasterBuilder b = DMSHelper.createRasterBuilder(proxy);
		if(b != null)
			return b.getLineHeightPixels();
		else
			return null;
	}

	/** Render a message to a raster graphic */
	protected RasterGraphic renderMessage(SignText st) {
		MultiString multi = new MultiString(st.getMulti());
		RasterGraphic[] pages = renderPages(multi);
		if(pages.length > 0)
			return pages[0];
		else
			return null;
	}

	/** Render the pages of a text message */
	protected RasterGraphic[] renderPages(MultiString ms) {
		Integer w = proxy.getWidthPixels();
		Integer h = getLineHeightPixels();
		Integer cw = proxy.getCharWidthPixels();
		Integer ch = proxy.getCharHeightPixels();
		if(w == null || h == null || cw == null || ch == null)
			return new BitmapGraphic[0];
		int df = DMSHelper.getDefaultFontNumber(proxy);
		RasterBuilder b = new RasterBuilder(w, h, cw, ch, df);
		return b.createPixmaps(ms);
	}

	/** Get the selected sign text message */
	protected SignText getSelectedSignText() {
		SignTextTableModel m = sign_text_model;
		if(m == null)
			return null;
		ListSelectionModel s = sign_text_table.getSelectionModel();
		return m.getProxy(s.getMinSelectionIndex());
	}

	/** Update one attribute on the form tab */
	public void updateAttribute(String a) {
		if(a == null || a.equals("defaultFont"))
			font_cmb.setSelectedItem(proxy.getDefaultFont());
		if(a == null || a.equals("awsAllowed"))
			awsAllowed.setSelected(proxy.getAwsAllowed());
		if(a == null || a.equals("awsControlled"))
			awsControlled.setSelected(proxy.getAwsControlled());
		// NOTE: messageCurrent attribute changes after all sign
		//       dimension attributes are updated.
		if(a == null || a.equals("messageCurrent"))
			selectSignText();
	}
}
