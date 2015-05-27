package cn.edu.thss.iise.beehivez.client.ui.modelpetriresourceallocation;


import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import cn.edu.thss.iise.beehivez.client.ui.ClientFrame;
import cn.edu.thss.iise.beehivez.client.ui.FunctionFramePlugin;
import cn.edu.thss.iise.beehivez.client.ui.customizableloggenerator.CustomizableLogGeneratorUI;
import cn.edu.thss.iise.beehivez.util.ResourcesManager;

/**
 * @author Wang Tang 2013.6.2
 * 
 */
public class ModelPetriResourceAllocationPlugin extends FunctionFramePlugin {

	//the serialVersionUID is generated by myself~~
	private static final long serialVersionUID = 3124123L;
	public ModelPetriResourceAllocationUI ui = null;
	ResourcesManager resourcesManager = new ResourcesManager();
	public ModelPetriResourceAllocationPlugin(ClientFrame mainframe) {
		super(mainframe);
		ui = new ModelPetriResourceAllocationUI();
		getModulePanel().add(ui, BorderLayout.CENTER);
		
	}

	

	@Override
	public Icon getIcon() {
		// TODO Auto-generated method stub
		String path = "/resources/images/Icon_LogGenerator.gif";
		String description = "Icon_LogGenerator";
		return new ImageIcon(getClass().getResource(path), description);
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return new String("ModelPetriResourceAllocation.plugin");
	}

	@Override
	public String getToolTip() {
		// TODO Auto-generated method stub
		return new String("ModePetriResourceAllocation.plugin");
	}

	@Override
	public void onLoad() {
		// TODO Auto-generated method stub
		
	}

}
