package org.js.mvc.demo.service.impl;

import org.js.mvc.autumn.annotation.Service;
import org.js.mvc.demo.service.IUserService;

/**
 * @author JiaShun
 * @date 2018/8/14
 */
@Service
public class UserServiceImpl implements IUserService {
    @Override
    public String findById(Integer id) {

        return id + ": Michael Jackson";
    }
}
