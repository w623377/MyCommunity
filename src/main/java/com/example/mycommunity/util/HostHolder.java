package com.example.mycommunity.util;

import com.example.mycommunity.entity.User;
import org.springframework.stereotype.Component;

/**
 * 持有用户信息,用于代替session对象.
 */
@Component
public class HostHolder {
    //这个类是为了代替session对象，因为session对象是线程不安全的，所以用ThreadLocal来代替，这样就可以保证线程安全了，每个线程都有自己的ThreadLocal，所以不会出现线程安全问题
    //ThreadLocal是一个线程局部变量，它的作用是为每个线程都提供一个变量的副本，使得每个线程在访问变量时，都是访问自己内部的副本，从而避免了多线程访问变量时的冲突。
    //当服务器接收到一个请求时，会创建一个线程来处理这个请求，这个线程会从ThreadLocal中取出当前用户的信息，这样就可以保证每个线程都有自己的用户信息，不会出现线程安全问题。
    private ThreadLocal<User> users = new ThreadLocal<>();//ThreadLocal是一个泛型类，这里的泛型是User

    public void setUser(User user) {
        users.set(user);
    }//set方法，将用户信息存入ThreadLocal中

    public User getUser() {
        return users.get();
    }//get方法，从ThreadLocal中取出用户信息

    public void clear() {
        users.remove();
    }//remove方法，清除ThreadLocal中的用户信息多个

}
