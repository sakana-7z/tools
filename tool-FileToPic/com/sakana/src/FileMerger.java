package com.sakana.src;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

public class FileMerger {
    public static void main(String[] args) {

        // 读取文件内容并赋值给 Config 类中的常量
        String toolFolder = "../tool-FileToPic";
        String inputFile = "inputAddress.txt";
        String filePath = toolFolder + File.separator + inputFile;

        String txtDirectoryPath = readFromFile(filePath);
        if (txtDirectoryPath != null) {
            Config.DIRECTORY_PATH = txtDirectoryPath;
        } else {
            System.err.println("文件 " + filePath + " 不存在或无法读取，请检查路径是否正确。");
            return;
        }




        // 获取文件夹路径
        String directoryPath = Config.DIRECTORY_PATH;

        // 获取文件夹目录下的图片(preview.jpg)路径
        String previewImagePath = directoryPath + File.separator + "preview.jpg";
        String alternativeImagePath = "com/sakana/img/ciallo.jpg";

        // 检查图片是否存在
        File previewImageFile = new File(previewImagePath);
        File alternativeImageFile = new File(alternativeImagePath);

        BufferedImage previewImage = null;
        if (previewImageFile.exists() && previewImageFile.isFile()) {
            try {
                previewImage = ImageIO.read(previewImageFile);
                if (previewImage == null) {
                    throw new IllegalArgumentException("无法读取图片：" + previewImagePath);
                }
            } catch (IOException e) {
                System.err.println("无法读取图片：" + previewImagePath);
                e.printStackTrace();
                return;
            }
        } else if (alternativeImageFile.exists() && alternativeImageFile.isFile()) {
            try {
                previewImage = ImageIO.read(alternativeImageFile);
                if (previewImage == null) {
                    throw new IllegalArgumentException("无法读取图片：" + alternativeImagePath);
                }
            } catch (IOException e) {
                System.err.println("无法读取图片：" + alternativeImagePath);
                e.printStackTrace();
                return;
            }
        } else {
            System.err.println("没有找到任何图片：" + previewImagePath + " 或 " + alternativeImagePath);
            return;
        }

        // 获取文件夹的名字，并构造输出文件路径
        Path dirPath = Paths.get(directoryPath);
        String folderName = dirPath.getFileName().toString();
        String outputImagePath = directoryPath + File.separator + folderName + ".jpg";

        // 创建临时ZIP文件
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream("temp.zip"))) {
            // 压缩文件夹中的所有文件
            compressFolder(zos, directoryPath, "");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 读取压缩文件内容
        byte[] zipContent = null;
        try {
            zipContent = Files.readAllBytes(Paths.get("temp.zip"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 读取图片内容
        byte[] imageData = null;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(previewImage, "jpg", baos);
            imageData = baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 创建新的preview.jpg，前部分为原始图像数据，后部分为ZIP内容
        byte[] finalData = new byte[imageData.length + zipContent.length];
        System.arraycopy(imageData, 0, finalData, 0, imageData.length);
        System.arraycopy(zipContent, 0, finalData, imageData.length, zipContent.length);

        // 写入最终的文件
        try (FileOutputStream fos = new FileOutputStream(outputImagePath)) {
            fos.write(finalData);
            System.out.println("文件合并成功！");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 删除临时ZIP文件
        File tempZipFile = new File("temp.zip");
        tempZipFile.delete();
    }


    private static String readFromFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            return reader.readLine(); // 假设文件只有一行内容
        } catch (IOException e) {
            System.err.println("文件 " + filePath + " 不存在或无法读取，请检查路径是否正确。");
            e.printStackTrace();
            return null;
        }
    }


    private static void compressFolder(ZipOutputStream zos, String folderPath, String parentPath) throws IOException {
        File folder = new File(folderPath);
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            String entryName = parentPath.isEmpty() ? file.getName() : parentPath + File.separator + file.getName();

            if (file.isDirectory()) {
                // 添加空目录条目
                ZipEntry entry = new ZipEntry(entryName + File.separator);
                zos.putNextEntry(entry);
                zos.closeEntry();

                // 递归压缩子文件夹
                compressFolder(zos, file.getAbsolutePath(), entryName);
            } else {
                // 添加文件条目
                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                }
                zos.closeEntry();
            }
        }
    }
}
