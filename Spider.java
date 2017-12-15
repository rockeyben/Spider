import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.PrintStream;

import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;


public class Spider {
   	private static String deanLogin = "http://dean.pku.edu.cn/student/authenticate.php";
   	private static String deanGrade = "http://dean.pku.edu.cn/student/new_grade.php?";
   	private static String deanCourse = "http://dean.pku.edu.cn/student/newXkInfo_1105.php?";
   	private static String deanCaptcha = "http://dean.pku.edu.cn/student/yanzheng.php?act=init";
	private static String submit = "%B5%C7%C2%BC";
	private static String parseDeanSession = "PHPSESSID=";
	private static String parseTime = "时间:";
	private static String parsePlace = "地点:";
	private static String parseTable = "</td><td>";
	private static String parseEndRow = "</td></tr><tr><td>";
	private static String deanRedir = "";
	private static String phps = "";
	private static String captchaPath = "E:/captcha.gif";
	private static String coursePath = "E:/course.txt";
	private HttpClient client;
	
	/* public Spider()
	 * 
	 * the construction function
	 * the HttpClient package use a HttpClient's object to execute various kind of actions like POST, GET
	 * so all we need to do is creating a HttpClient's object for our following usage
	 */
	public Spider(){
		client = new DefaultHttpClient();
	}
	
	/* public boolean getCaptchaFunc() 
	 * 
	 * If we want to login PKU' dean system, we need to post a captcha to the website
	 * So we need to download the captcha from the original login window of PKU' dean system
	 * we save the captcha in a gif format, and display it in our client interface
	 */
	public boolean getCaptchaFunc() throws Exception{
		HttpGet getCaptcha = null; // it is a format of request : GET
		HttpResponse gifResponse = null; // it is used to get response from the website
		HttpEntity entityCaptcha = null;// it is what contained in response, in this function, it is the binary data of a gif picture 
		
		/* this try-catch code block is used to get captcha from website
		 * 
		 * if network link failed,
		 * exception will be caught by programme and return a false to indicate that we failed to get the captcha
		 */
		try{
			getCaptcha = new HttpGet(deanCaptcha);
			gifResponse = client.execute(getCaptcha);
			entityCaptcha = gifResponse.getEntity();
		}
		catch(Exception e){
			e.printStackTrace();
			return false;
		}
		
		if(entityCaptcha == null) // if the entity is empty, return false
			return false;
		
		/* now we get the captcha,
		 * then we need to save it in a local gif file
		 */
		InputStream in = entityCaptcha.getContent();
		File file = new File(captchaPath);
		FileOutputStream out = new FileOutputStream(file);
		int i = -1;
		byte[] byt = new byte[1024];
		while((i = in.read(byt)) != -1){
			out.write(byt);
		}
		in.close();
		out.close();
		return true;
	}
	
	/* public boolean deanLogin
	 * 
	 * In this function, we mock login process of PKU' dean system
	 * we need client's name, password, and the captcha which is input by client by hand
	 * if we login successfully, we can get the redirection webpage of dean's system
	 * which is customized for each client specificly
	 */
	public boolean deanLogin(String name, String password, String captcha) throws Exception{
		
		HttpContext context = new BasicHttpContext();
		HttpPost request = new HttpPost(deanLogin);
		
		/* set POST request 
		 * according to our analysize, we only need 4 params to mock a login post request, 
		 * which are named as 'sno', 'password', 'captcha', 'submit'
		 * so we use NameValuePair to fullfill this form
		 */
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("sno", name));
		params.add(new BasicNameValuePair("password", password));
		params.add(new BasicNameValuePair("captcha", captcha));
		params.add(new BasicNameValuePair("submit", submit));
		request.setEntity(new UrlEncodedFormEntity(params));
		
		/* execute POST */
		HttpResponse response = null;
		try{
			response = client.execute(request, context);
		}
		catch(Exception e){
			System.out.println(e.getMessage());
			e.printStackTrace();
			return false;
		}
		
		System.out.println("status:" + response.getStatusLine().getStatusCode());
		
		HttpEntity entity = response.getEntity();
		String postResult = EntityUtils.toString(entity, "utf-8");
		/* check if the client input wrong usrname or password of captcha  */
		if(postResult.indexOf("PHPSESSID") == -1){
			if(postResult.indexOf("学号不存在，或者密码有误") != -1)
				return false;
			else if(postResult.indexOf("图形校验码错误") != -1)
				return false;
		}
		CookieStore cookieStore = ((AbstractHttpClient) client).getCookieStore();
		
		/* if we login successfully,
		 * the login interface return a PHPSESSID
		 * we use this PHPSESSID to redirect to client's customized homepage
		 */
		int idx = postResult.indexOf(parseDeanSession);
		int idx2 = postResult.indexOf("\"<", idx);
		String phps  = postResult.substring(idx, idx2);
		deanRedir = deanCourse + phps;
		return true;
	}
	
	public String processCourse(String _c){
		String res = "";
		String[] c = _c.split(": ");
		//System.out.println(c[0] + c[1] + c[2]);
	    //the curriculum doesn't have a time, need to be added by hand
	    if (c.length < 2)
	    {
	        return null;
	    }
	    //System.out.println(c[0] + c[1] + c[2]);
	    res = c[0];
	    //split the time according to ","
	    String[] t = c[1].split(",");
	    for (int i = 0; i < t.length; i++)
	    {
	        if (t[i].length() > 2)
	        {
	        	res += ",";
	            if (t[i].charAt(0) == '周')
	            {
                    int weekday, num_s, num_e;
                    /*
                    19968
                    20108
                    19977
                    22235
                    20116
                    20845
                     */
                    switch ((int) t[i].charAt(1))
                    {
                        case 19968:
                            weekday = 0;
                            break;
                        case 20108:
                            weekday = 1;
                            break;
                        case 19977:
                            weekday = 2;
                            break;
                        case 22235:
                            weekday = 3;
                            break;
                        case 20116:
                            weekday = 4;
                            break;
                        case 20845:
                            weekday = 5;
                            break;
                        default:
                            weekday = 6;
                    }
                    
                    res += weekday + "-";
                    
                    int index = 2;
                    num_s = 0;
                    num_e = 0;
                    while (t[i].charAt(index) != '-')
                    {
                        num_s *= 10;
                        num_s += t[i].charAt(index) - '0';
                        index++;
                    }
                    index++;
                    while (index < t[i].length() && t[i].charAt(index) != '(')
                    {
                        num_e *= 10;
                        num_e += t[i].charAt(index) - '0';
                        index++;
                    }
                    if (num_e == 0)
                    {
                        num_e = num_s;
                    }
                    res += (num_s + "-" + num_e + "-");
                    //single week or double week or every week

                    if (index < t[i].length() && t[i].charAt(index) == '(')
                    {
                        index++;
                        if (t[i].charAt(index) == '单')
                        {
                        	res += "S";
                        } else
                        {
                        	res += "D";
                        }
                    }
                    else{
                    	res += "E";
                    }
	            }
	        }
	    }
	    System.out.println(res);
	    return res;
	}
	
	
	/* public List<String> getCourse
	 * 
	 * in this function, we get course list from students' dean webpage
	 * and decode it into our target form, save it in local
	 */
	public List<String> getCourse() throws Exception{
		HttpGet getMsg = null;
		HttpResponse courseResponse = null;
		/* get the web page which contains our course information */
		try{
			getMsg = new HttpGet(deanRedir);
			courseResponse = client.execute(getMsg);	
		}catch(Exception e){
			System.out.println(e.getMessage());
			e.printStackTrace();
			return null;
		}
		HttpEntity entityCourse = courseResponse.getEntity();
		String courseRaw = EntityUtils.toString(entityCourse, "UTF-8");
		
		/* now we start to decode */
		List<String> courseList = new ArrayList<String>();
		String courseInfo = "";
		int idxStart = 0;
		int idx = -1;
		int idx2 = -1;
		while(idxStart != -1){
			idx = courseRaw.indexOf(parseTable, idxStart) + parseTable.length();
			idx2 = courseRaw.indexOf(parseTable, idx);
			courseInfo = courseRaw.substring(idx, idx2);

			idx = courseRaw.indexOf(parseTime, idx2);
			idx2 = courseRaw.indexOf("<br/>", idx);
			courseInfo += courseRaw.substring(idx+2, idx2);
			
			idx = courseRaw.indexOf(parsePlace, idx2);
			idx2 = courseRaw.indexOf(parseTable, idx);
			courseInfo +=  courseRaw.substring(idx+2, idx2);
			
			idxStart = courseRaw.indexOf(parseEndRow, idx2);
			courseList.add(courseInfo);
		}
		
		/* now we start to save it to local */
		try{
			File file = new File(coursePath);
			PrintStream ps = new PrintStream(new FileOutputStream(file));
			for(String cour : courseList){
				//System.out.println(cour);
				String saveCour = processCourse(cour);
				if(saveCour != null)
					ps.println(processCourse(cour));
			}
		}catch(FileNotFoundException e){
			e.printStackTrace();
		}

		return courseList;
	}
	
	
	/* public List<String> getGrade()
	 * 
	 * Similar to method getCourse(), this method can get your course grade from website
	 * but we don't use it in our main class
	 */
	public List<String> getGrade() throws Exception{
		deanRedir = deanGrade + phps;
		HttpGet getGrade = new HttpGet(deanRedir);
		HttpResponse gradeResponse = client.execute(getGrade);
		HttpEntity entityGrade = gradeResponse.getEntity();
		String gradeRaw = EntityUtils.toString(entityGrade, "UTF-8");
		
		List<String> gradeList = new ArrayList<String>();
		int idxStart = gradeRaw.indexOf("<tr><td>") + "<tr><td>".length();
		int idx, idx2;
		while(true){
			idx = gradeRaw.indexOf("<tr><td>", idxStart);
			if(idx == -1)
				break;
			idx += "<tr><td>".length();
			String gradeInfo = "";
			int k = 7;
			while(k > 0){
				k--;
				idx2 = gradeRaw.indexOf("</td><td>", idx);
				gradeInfo += gradeRaw.substring(idx, idx2) + " ";
				idx = idx2 + "</td><td>".length();
			}
			idx2 = gradeRaw.indexOf("</td>", idx);
			gradeInfo += gradeRaw.substring(idx, idx2);
			System.out.println(gradeInfo);
			gradeList.add(gradeInfo);
			idxStart = idx2;
		}

		return gradeList;
	}
	
	
	public static void main(String args[]) throws Exception{
		/* only for test */ 
		Spider spider = new Spider();
		spider.getCaptchaFunc();
		Scanner sc = new Scanner(System.in);
		String myname = sc.next();
		String mypass = sc.next();
		String capthca = sc.next();
		if(spider.deanLogin(myname, mypass, capthca)){
			List<String> a= spider.getCourse();
		}
		return ;
	}
}
