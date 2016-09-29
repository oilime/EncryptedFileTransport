package com.lanan.filetransport.chat;

class ChatMsgEntity {
    private String name;
    private String date;
    private String message;
    private boolean isComMeg = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    String getDate() {
        return date;
    }

    void setDate(String date) {
        this.date = date;
    }

    String getMessage() {
        return message;
    }

    void setMessage(String message) {
        this.message = message;
    }

    boolean getMsgType() {
        return isComMeg;
    }

    void setMsgType(boolean isComMsg) {
        isComMeg = isComMsg;
    }
}