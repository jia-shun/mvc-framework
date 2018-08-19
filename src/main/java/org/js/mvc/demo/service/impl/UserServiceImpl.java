package org.js.mvc.demo.service.impl;

import org.js.mvc.autumn.annotation.Service;
import org.js.mvc.demo.service.IUserService;

/**
 * Created by JiaShun on 2018/8/16.
 */
@Service
public class UserServiceImpl implements IUserService {

    public String findById(Integer id) {

        return id + ": Michael Jackson";
    }
}
