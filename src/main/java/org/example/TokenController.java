package org.example;

import org.example.annotations.NotRepeatSubmit;
import org.example.entity.*;
import org.example.utils.MD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/token")
public class TokenController {

    @Autowired
    private RedisTemplate redisTemplate;


    @PostMapping("/api_token")
    public ApiResponse<AccessToken> apiToken(String appId, @RequestHeader("timestamp") String timestamp, @RequestHeader("sign") String sign) {

        // 参数错误
        if (StringUtils.isEmpty(appId) || StringUtils.isEmpty(timestamp) || StringUtils.isEmpty(sign)) {
            return ApiResponse.error(ApiCodeEnum.PARAMETER_ERROR.getCode(), ApiCodeEnum.PARAMETER_ERROR.getMsg());
        }

        // 请求超时
        long reqeustInterval = System.currentTimeMillis() - Long.valueOf(timestamp);
        if (reqeustInterval > 15 * 60 * 1000) {
            return ApiResponse.error(ApiCodeEnum.REQUEST_TIMEOUT.getCode(), ApiCodeEnum.REQUEST_TIMEOUT.getMsg());
        }

        // 1. 根据appId查询数据库获取appSecret
        AppInfo appInfo = new AppInfo("asdf", "12345678954556");

        // 2. 校验签名
        String signString = timestamp + appId + appInfo.getKey();
        String signature = MD5Util.encode(signString);

        // 校验签名
        if (!signature.equals(sign)) {
            return ApiResponse.error(ApiCodeEnum.SIGN_ERROR.getCode(), ApiCodeEnum.SIGN_ERROR.getMsg());
        }

        // 3. 如果正确生成一个token保存到redis中，如果错误返回错误信息
        AccessToken accessToken = this.saveToken(0, appInfo, null);
        return ApiResponse.success(accessToken);
    }

    @NotRepeatSubmit(5000)
    @PostMapping("user_token")
    public ApiResponse<UserInfo> userToken(String username, String password) {
        // 根据用户名查询密码, 并比较密码(密码可以RSA加密一下)
        UserInfo userInfo = new UserInfo(username, "81255cb0dca1a5f304328a70ac85dcbd", "111111");
        String pwd = password + userInfo.getSalt();
        String passwordMD5 = MD5Util.encode(pwd);
        Assert.isTrue(passwordMD5.equals(userInfo.getPassword()), "密码错误");

        // 2. 保存Token
        AppInfo appInfo = new AppInfo("1", "12345678954556");
        AccessToken accessToken = this.saveToken(1, appInfo, userInfo);
        userInfo.setAccessToken(accessToken);
        return ApiResponse.success(userInfo);
    }

    private AccessToken saveToken(int tokenType, AppInfo appInfo, UserInfo userInfo) {

        String token = UUID.randomUUID().toString();
        System.out.println("token: " + token);

        // token有效期为2小时
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.SECOND, 7200);
        Date expireTime = calendar.getTime();

        // 4. 保存token
        ValueOperations<String, TokenInfo> operations = redisTemplate.opsForValue();
        TokenInfo tokenInfo = new TokenInfo();
        tokenInfo.setTokenType(tokenType);
        tokenInfo.setAppInfo(appInfo);

        if (tokenType == 1) {
            tokenInfo.setUserInfo(userInfo);
        }

        operations.set(token, tokenInfo, 7200, TimeUnit.SECONDS);
        AccessToken accessToken = new AccessToken(token, expireTime);
        return accessToken;
    }

}
