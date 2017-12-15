# 比较naive的爬虫

因为图形界面不是我负责……所以据说运行之后会把验证码存到E盘然后点开验证码再输入就可以查到课表与成绩了orz……

1.
获取验证码的函数为
public boolean getCaptchaFunc()
返回true表示验证码下载成功

2.
登录函数为
public boolean deanLogin(String name, Sring password, String captcha)
返回true表示登录成功

3.
获取课程函数为
public List<String> getCourse()
在deanLogin成功之后调用，返回课程信息