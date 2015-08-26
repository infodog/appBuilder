package net.xinshi.appbuilder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: mac
 * Date: 13-4-29
 * Time: 下午3:45
 * To change this template use File | Settings | File Templates.
 */
public class Main {
    static public void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("appBuilder inputDir outputZip");
            return;
        }
        String inputDir = args[0];
        String outputZip = args[1];

        //遍历所有的文件，对jsxp和css结尾的文件进行替换
        File f = new File(inputDir);
        if (!f.isDirectory()) {
            System.out.println(inputDir + " 不是一个目录。");
            return;
        }

        File outFile = new File(outputZip);
        if (outFile.isDirectory()) {
            System.out.println(outputZip + " 是一个目录。");
            return;
        }

        ZipOutputStream zipos = new ZipOutputStream(new FileOutputStream(outFile));
        add(zipos, f, f);
        zipos.close();

    }

    static Pattern pattern = Pattern.compile("\"([\\.]+/[^\"><]+)\"|\"(res/[^\"><]+)\"|\\(([\\.]+/[^\"><)]+)\\)|url\\(([^\"'><\\)]+)\\)|url\\(\"([^><\\)]+)\"\\)|url\\('([^><\\)]+)'\\)");   //("[\.]+/[^"><]+")|("res/[^"><]+")|\([\.]+/[^"><)]+\)

    static String getUrl(Matcher m){
        for(int i=1; i<m.groupCount();i++){
            String url = m.group(i);
            if(url!=null){
                return url;
            }
        }
        return null;
    }
    static void add(ZipOutputStream zipOutputStream, File f, File root) throws Exception {
        String rootPath = root.getCanonicalPath();
        if (!f.isDirectory()) {
            byte[] byteContent = null;
            if (StringUtils.endsWithAny(f.getCanonicalPath(), new String[]{".jsxp", ".html",".htm",".css"})) {
                File parent = f.getParentFile();
                String content = FileUtils.readFileToString(f, "utf-8");
                StringBuffer sb = new StringBuffer();
                Matcher m = pattern.matcher(content);
                while (m.find()) {
                    String url = getUrl(m);
                    String wholeMatch = m.group();
                    if(StringUtils.contains(wholeMatch,".jsx")){
                        //对于指向.jsx的文件不做替换
                        m.appendReplacement(sb, wholeMatch);
                        continue;
                    }
                    try {
                        File realFile = new File(parent, url);
                        String fullPath = realFile.getCanonicalPath();
                        if (realFile.exists()) {
                            String relative = StringUtils.right(fullPath, fullPath.length() - rootPath.length());
                            relative = StringUtils.replace(relative, "\\", "/");
                            if (relative.startsWith("/")) {
                                relative = relative.substring(1);
                            }
                            relative = "@{" + relative + "}@";
                            wholeMatch = wholeMatch.replace(url,relative);
                            m.appendReplacement(sb, wholeMatch);
                        } else {
                            m.appendReplacement(sb, wholeMatch);
                        }

                    } catch (Exception e) {
                        System.out.println("error url=" + url );
                        e.printStackTrace();
                    }
                }
                m.appendTail(sb);
                content = sb.toString();
                byteContent = content.getBytes("utf-8");
            } else {
                byteContent = FileUtils.readFileToByteArray(f);
            }
            String fileEntry = f.getCanonicalPath();
            String relative = StringUtils.right(fileEntry, fileEntry.length() - rootPath.length());
            relative = StringUtils.replace(relative, "\\", "/");
            if (relative.startsWith("/")) {
                relative = relative.substring(1);
            }
            System.out.println(relative);
            zipOutputStream.putNextEntry(new ZipEntry(relative));
            zipOutputStream.write(byteContent);
        } else if (f.isDirectory()) {
            for (File child : f.listFiles()) {
                add(zipOutputStream, child, root);
            }
        }
    }


}
