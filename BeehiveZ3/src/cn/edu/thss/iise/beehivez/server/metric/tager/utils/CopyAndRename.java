package cn.edu.thss.iise.beehivez.server.metric.tager.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.processmining.framework.models.petrinet.PetriNet;

public class CopyAndRename {
	
	public static final String ROOT_FOLDER = "D:\\百度云同步盘\\Learn@Tsinghua\\过程数据组\\Tager\\08.性质符合实验\\";
	public static final File TARGET_FOLDER = new File("E:\\Downloads\\");
	
	public static final String[] PROPERTY_FOLDERS = {
		ROOT_FOLDER + "01.顺序结构漂移不变性\\",
		ROOT_FOLDER + "02.跨度负相关性\\",
		ROOT_FOLDER + "03.无关任务递减性\\",
		ROOT_FOLDER + "04.循环长度负相关性\\",
		ROOT_FOLDER + "05.互斥结构漂移不变性\\",
		ROOT_FOLDER + "06.循环结构漂移不变性\\",
		ROOT_FOLDER + "07.并发结构漂移不变性\\",
//		ROOT_FOLDER + "08.不平衡性\\"
	};

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		for(int i = 0; i < PROPERTY_FOLDERS.length; ++i) {
			String modelsFolder = PROPERTY_FOLDERS[i] + "Models\\";
			for(int j = 1; ; ++j) {
				File file = new File(modelsFolder + "N" + j + ".pnml");
				if(!file.exists()) {
					break;
				}
				String target = "N" + (i + 1) + "-" + j + ".pnml";
				CopyAndRename.copyFile(file, TARGET_FOLDER, target);
			}
		}
	}
	
	/** 
     * 复制文件(以超快的速度复制文件) 
     *  
     * @param srcFile 
     *            源文件File 
     * @param destDir 
     *            目标目录File 
     * @param newFileName 
     *            新文件名 
     * @return 实际复制的字节数，如果文件、目录不存在、文件为null或者发生IO异常，返回-1 
     */  
    public static long copyFile(File srcFile, File destDir, String newFileName) {  
        long copySizes = 0;  
        if (!srcFile.exists()) {  
            System.out.println("源文件不存在");  
            copySizes = -1;  
        } else if (!destDir.exists()) {  
            System.out.println("目标目录不存在");  
            copySizes = -1;  
        } else if (newFileName == null) {  
            System.out.println("文件名为null");  
            copySizes = -1;  
        } else {  
            try {  
                FileChannel fcin = new FileInputStream(srcFile).getChannel();  
                FileChannel fcout = new FileOutputStream(new File(destDir,  
                        newFileName)).getChannel();
                long size = fcin.size();
                fcin.transferTo(0, fcin.size(), fcout);  
                fcin.close();  
                fcout.close();  
                copySizes = size;  
            } catch (FileNotFoundException e) {  
                e.printStackTrace();  
            } catch (IOException e) {  
                e.printStackTrace();  
            }  
        }  
        return copySizes;  
    }  

}
