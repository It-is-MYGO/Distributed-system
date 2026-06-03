package com.example.distributedsystem.dto;

public class UserUpdateRequest {
    private String nickName;
    private String introduceSign;
    private String address;
    private String avatarUrl;

    public UserUpdateRequest() {}

    public UserUpdateRequest(String nickName, String introduceSign, String address) {
        this.nickName = nickName;
        this.introduceSign = introduceSign;
        this.address = address;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getIntroduceSign() {
        return introduceSign;
    }

    public void setIntroduceSign(String introduceSign) {
        this.introduceSign = introduceSign;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
