package com.hxd;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.google.gson.Gson;
import com.hxd.bean.SSRNode;
import com.hxd.gson.GUIConfig;
import com.hxd.gson.Server;

public class FreeSSR {
	
	//1. 爬取地址/逗比根据地免费账号地址
	static String SourceUrl = "https://doub.bid/sszhfx/";
	
	//2. SSR可执行程序所在位置,可执行程序名称必须是ShadowsocksR-dotnet4.0.exe
	static String InstallFolder = "C:\\GreenSoftware\\ShadowsocksR-4.7.0\\ShadowsocksR-dotnet4.0.exe";
	
	//3. SSR的json配置文件路径
 	static String filePath = "C:\\GreenSoftware\\ShadowsocksR-4.7.0\\gui-config.json";
	
 	//4. 节点状态json地址
	static String NodeStatusUrl = "http://sstz.toyoo.ml/json/stats.json";
	
	
	/*
	 * base64解码
	 */
	public static String base64Decode(String string) throws UnsupportedEncodingException {
		byte[] asBytes = Base64.getUrlDecoder().decode(string);
		String result = new String(asBytes,"UTF-8");
		return result;
	}
	
	/*
	 * 根据正则表达式提取字符串
	 */
	public static List<String> getStringByRegex(String regex,String source){
		List<String> list = new ArrayList<>();
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(source);
		while(matcher.find()) {
			list.add(matcher.group());
		}
		return list;
	}
	
	/*
	 * 获取节点可用状态
	 */
	public static List<Boolean> getNodeStatus() throws IOException {
		Document doc = Jsoup.connect(NodeStatusUrl)
				.ignoreContentType(true)
				.get();
		String json  = doc.select("body").text();
		
		//直接用正则表达式取状态数据
		List<String> statusList = new ArrayList<>();
		statusList = getStringByRegex("(\"status\": ){1}[a-z]*", json);
		//转换为布尔值
		List<Boolean> status = new ArrayList<>();
		for (String string : statusList) {
			string = string.substring(10);
			status.add(Boolean.valueOf(string));
		}
		return status;
	}
	
	/*
	 * 执行ping操作,判断节点是否可用,及延时时间
	 */
	public static String getPingTime(String ip) throws IOException {
		//执行ping命令
		Process p = Runtime.getRuntime().exec("ping "+ ip);
		//接受返回的数据
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(),"GBK"));
		String line;
		StringBuilder sb = new StringBuilder();
		while((line=br.readLine())!=null) {
			//System.out.println(line);
			sb.append(line);
		}
		//使用正则表达式ping结果
		List<String> result = FreeSSR.getStringByRegex("[0-9]*ms$", sb.toString());
		//长度为零,及结果中不包含"平均 = XXXms"的即ping不通
		if(result.size()==0) {
			return "请求超时";
		}else {
			return result.get(0);
		}
	}
	
	/*
	 * 读取json配置文件
	 * 参数:文件路径
	 */
	public static String readJSON(String filePath) throws IOException {
		//文件编码
		String encoding = "UTF-8";
		//指定文件地址
	    File file = new File(filePath);
	    //存储文件内容
	    StringBuilder sb  = new StringBuilder();
	    //判断文件是否存在
	    if (file.isFile() && file.exists()) { 
	        InputStreamReader read = new InputStreamReader(
	                new FileInputStream(file), encoding);
	        BufferedReader bufferedReader = new BufferedReader(read);
	        String lineTxt = null;
	        while ((lineTxt = bufferedReader.readLine()) != null) {
	               sb.append(lineTxt+"\n");
	         }
	        read.close();
	    } else {
	        System.out.println("找不到指定的文件");
	    }
	    String result = sb.toString();
	    return result;
	} 
	
	/*
	 * 将更新的内容写入json配置文件
	 */
	public static boolean writeJSON(String result,String filePath) {
			File file = new File(filePath);
			BufferedWriter writer = null;
			try {
				writer = new BufferedWriter(new OutputStreamWriter(  
				        new FileOutputStream(file), "UTF-8"));
					writer.write(result);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}finally {
				try {
					writer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}  				
			}
		    return true;
	}
	
	/*启动ShadowsocksR-dotnet4.0.exe
	 * 判断SSR程序是否运行,运行就杀掉进程
	 * cmd命令
	 * 杀掉程序:taskkill /f /im ShadowsocksR-dotnet4.0.exe
	 * 判断程序是否运行:tasklist|find /i "ShadowsocksR-dotnet4.0.exe"
	 * 启动程序:C:\GreenSoftware\ShadowsocksR-4.7.0\ShadowsocksR-dotnet4.0.exe
	 */
	public static void startSSR() throws IOException {
		//启动命令
		String startCommand = InstallFolder;
		//判断,查找有没有ssr进程,不加"cmd /c"会报错
		String findCommand = "cmd /c tasklist|findstr /i \"ShadowsocksR-dotnet4.0\"";
		//杀死进程命令
		String killCommand = "taskkill /f /im ShadowsocksR-dotnet4.0.exe";
		
		Runtime run = Runtime.getRuntime();
		//先判断SSR是否已运行
		Process process = run.exec(findCommand);
		//接受返回信息
		BufferedReader  bufferedReader = new BufferedReader  
	            (new InputStreamReader(process.getInputStream()));
		String line = null;
		//存储返回信息
		StringBuilder sb = new StringBuilder();
		 while ((line = bufferedReader.readLine()) != null) {  
	            sb.append(line + "\n");  
		 }
		 //System.out.println(sb);
		 //从反馈的信息中进行判断,包含"ShadowsocksR-dotnet4.0"代表SSR程序已启动
		 String regex = "ShadowsocksR-dotnet4.0";
		 int count = FreeSSR.getStringByRegex(regex, sb.toString()).size();
		 if(count == 2) {
			 //重启,先杀再启
			 System.out.println("SSR客户端已启动,将进行重启");
			 run.exec(killCommand);
			 //延时,等待程序退出再重新启动
			 try {
				 System.out.println("请稍候......");
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 run.exec(startCommand);
			 System.out.println("重启完成");
		 }else {
			 //否则直接启动SSR客户端
			 System.out.println("启动SSR客户端");
			 run.exec(startCommand);
		 }
	}
	
	/*
	 * 退出程序
	 */
	public static void exit() {
		//退出
		System.out.println("\n按回车键退出");
		Scanner input = new Scanner(System.in);
		String isExit = input.nextLine();
		if(isExit.length() == 0) {
			//关闭当前进程
			input.close();
			System.exit(0);
			}
	}
	
	public static void main(String[] args) throws IOException {
		
		//开始时间
		long startTime = System.currentTimeMillis();
		
		
		System.out.println(">>>>>>>> 开始连接......");
		
		Document doc = Jsoup.connect(SourceUrl).get();
		
		System.out.println("连接成功!");

		//更新日期
		Elements updateTimeEle = doc.select("span[style='color: #ff6464;']");
		String updateTime = updateTimeEle.eq(1).text();
		System.out.println("更新日期: "+updateTime);
		
		//账号列表元素
		Elements tableEle = doc.select("table[width=100%]");
		
		//System.out.println(tableEle.html());
		
		//将元素转换字符串
		String tableStr = tableEle.toString();
		
		//正则表达式匹配SSR链接规则
		String regex = "(ssr://){1}[a-zA-Z0-9_]{60,}";
		List<String> SSRList = new ArrayList<>();
		//提取SSR链接
		System.out.println(">>>>>>>> 获取SSR地址......");
		SSRList = getStringByRegex(regex, tableStr);
		System.out.println("共获得"+ SSRList.size() +"个账号");
		//如果没有可用节点,直接停止程序
		if(SSRList.size() == 0) {
			System.out.println("***** 无可用节点,停止程序! *****");
			exit();
		}
		
		
		System.out.println(">>>>>>>> 对地址进行base64解码......");
		
		String urlString = "";
		String[] urlArray;
		//存储节点
		List<SSRNode> nodeList = new ArrayList<>();
		//获取节点名
		for(int i = 1;i<tableEle.select("tr").size();i++) {
			String serverName = tableEle.select("tr").get(i).select("td").get(0).text();
			//去除ssr://
			urlString = SSRList.get(i-1);
			urlString = urlString.substring(6);
			//BASE64解码
			urlString = base64Decode(urlString);
			urlArray = urlString.split(":");
			//SSRNode节点
			SSRNode ssrNode = new SSRNode();
			//如果数组长度为11的IP地址是IPV6,长度为6的是IPV4
			if(urlArray.length > 6) {
				String ip = urlArray[0] 
							+ ":" + urlArray[1] 
							+ ":" +urlArray[2] 
							+ ":" + urlArray[3]
							+ ":" + urlArray[4]
							+ ":" + urlArray[5];
				//备注
				ssrNode.setRemarks(serverName);
				//ip
				ssrNode.setServer(ip);
				//端口
				ssrNode.setServer_port(Integer.valueOf(urlArray[6]));
				//协议
				ssrNode.setProtocol(urlArray[7]);
				//加密方式
				ssrNode.setMethod(urlArray[8]);
				//混淆
				ssrNode.setObfs(urlArray[9]);
				//密码
				String pdStr = getStringByRegex("[a-zA-Z0-9]*", urlArray[10]).get(0);
				//对密码进行base64二次解码
				pdStr = base64Decode(pdStr);
				ssrNode.setPassword(pdStr);
				//remarks
				String remStr = getStringByRegex("(=){1}[a-zA-Z0-9-]*", urlArray[10]).get(0);
				remStr = remStr.substring(1);
				ssrNode.setRemarks_base64(remStr);
				nodeList.add(ssrNode);
			}else {
				String ip = urlArray[0];
				//备注
				ssrNode.setRemarks(serverName);
				//ip
				ssrNode.setServer(ip);
				//端口
				ssrNode.setServer_port(Integer.valueOf(urlArray[1]));
				//协议
				ssrNode.setProtocol(urlArray[2]);
				//加密方式
				ssrNode.setMethod(urlArray[3]);
				//混淆
				ssrNode.setObfs(urlArray[4]);
				//密码
				String pdStr = getStringByRegex("[a-zA-Z0-9]*", urlArray[5]).get(0);
				//对密码进行base64二次解码
				pdStr = base64Decode(pdStr);
				ssrNode.setPassword(pdStr);
				//remarks
				String remStr = getStringByRegex("(=){1}[a-zA-Z0-9-]*", urlArray[5]).get(0);
				remStr = remStr.substring(1);
				ssrNode.setRemarks_base64(remStr);
				nodeList.add(ssrNode);
			}
		
		}
		System.out.println("base64解码完成");	
		
		//查询节点状态
		List<Boolean> statusList = getNodeStatus();
	 	for (int i = 0; i < statusList.size();i++) {
			nodeList.get(i).setStatus(statusList.get(i));
		}
		
	 	List<SSRNode> okNodeList = new ArrayList<>();
	 	//进行ping操作
	 	System.out.println(">>>>>>>> 执行ping操作中......");
	 	for (SSRNode nl : nodeList) {
			if(nl.isStatus()) {
				System.out.println("ping " + nl.getServer() + "......");
				String result = getPingTime(nl.getServer());
				nl.setAvgPingTime(result);
				okNodeList.add(nl);
			}
		}
	 	//根据延时进行排序
	 	Collections.sort(okNodeList);
	 	
	 	//读取配置文件
	 	String result = readJSON(filePath);
	 	//利用GSON解析json
	 	Gson gson = new Gson();
	 	GUIConfig guiConfig = gson.fromJson(result, GUIConfig.class);
	 	//服务器列表
	 	List<Server> configList = new ArrayList<>();
	 	//信息显示
		System.out.println(okNodeList.size()+"个可用节点:");
		int nodeCount = 0;
		for (SSRNode sn : okNodeList) {
			System.out.println("======== 第"+ ++nodeCount +"个节点 ========");
			System.out.println(sn.toString());
			//服务器
		 	Server config = new Server();
			//设置 备注  ip  端口  协议  加密方式  混淆  密码  remarks_base 分组
			config.setServer(sn.getServer());
			config.setServer_port(sn.getServer_port());
			config.setProtocol(sn.getProtocol());
			config.setMethod(sn.getMethod());
			config.setObfs(sn.getObfs());
			config.setPassword(sn.getPassword());
			config.setRemarks_base64("");
			config.setGroup(sn.getRemarks());
			//用平均延时做备注
			config.setRemarks(sn.getAvgPingTime());
			configList.add(config);
		}
		//更新服务器列表
		guiConfig.setConfigs(configList);
		//将更新后的信息装换为json
		String updateConfig = gson.toJson(guiConfig);
		
		//System.out.println(updateConfig);
		
		System.out.println(">>>>>>>> 更新配置文件......");
		
		//写入配置文件
		writeJSON(updateConfig, filePath);
		
		System.out.println("完成更新");
		
		//启动或重启以应用新的配置文件
		startSSR();
		
		System.out.println();
		//结束时间
		long endTime = System.currentTimeMillis();
		System.out.println("耗时:"+ (endTime-startTime)*1.0/1000 + " 秒");
		
		//退出程序
		exit();
	}
}

