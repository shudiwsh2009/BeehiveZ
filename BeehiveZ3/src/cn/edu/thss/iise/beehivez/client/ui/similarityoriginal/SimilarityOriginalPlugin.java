/**
 * BeehiveZ is a business process model and instance management system.
 * Copyright (C) 2011  
 * Institute of Information System and Engineering, School of Software, Tsinghua University,
 * Beijing, China
 *
 * Contact: jintao05@gmail.com 
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation with the version of 2.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package cn.edu.thss.iise.beehivez.client.ui.similarityoriginal;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import cn.edu.thss.iise.beehivez.client.ui.ClientFrame;
import cn.edu.thss.iise.beehivez.client.ui.FunctionFramePlugin;
import cn.edu.thss.iise.beehivez.client.ui.miningevaluate.MiningEvaluateUI;
import cn.edu.thss.iise.beehivez.util.ResourcesManager;

public class SimilarityOriginalPlugin extends FunctionFramePlugin{
	private static final long serialVersionUID = 123L;
	public SimilarityOriginalUI ui = null;
	ResourcesManager resourcesManager = new ResourcesManager();
		
	
	public SimilarityOriginalPlugin(ClientFrame mainframe) {
		super(mainframe);
		ui = new SimilarityOriginalUI();
		getModulePanel().add(ui, BorderLayout.CENTER);
		getModulePanel().setBackground(Color.black);
	}

	

	@Override
	public void onLoad() {
		
	}

	@Override
	public String getName() {
		return resourcesManager.getString("SimilarityOriginal.plugin");
	}

	@Override
	public Icon getIcon() {
		String path = "/resources/images/Icon_SimilarityAgainstOriginalModel.gif";
		String description = "Icon_IndexManagement";
		return new ImageIcon(getClass().getResource(path), description);
	}

	@Override
	public String getToolTip() {
		return resourcesManager.getString("SimilarityOriginal.plugin");
	}

}
