package org.js.mvc.demo.controller;

import org.js.mvc.autumn.annotation.Autowired;
import org.js.mvc.autumn.annotation.Controller;
import org.js.mvc.autumn.annotation.RequestMapping;
import org.js.mvc.autumn.annotation.RequestParam;
import org.js.mvc.demo.service.IUserService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author JiaShun
 * @date 2018/8/14
 */
@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private IUserService userService;

    @RequestMapping("/findById")
    public void findById(HttpServletRequest req, HttpServletResponse resp, @RequestParam("id") Integer id){
        String result = userService.findById(id);
        try {
            resp.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
