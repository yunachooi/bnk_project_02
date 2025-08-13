package com.example.bnk_project_02s.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.bnk_project_02s.dto.ShoppingProductsDto;
import com.example.bnk_project_02s.entity.User;
import com.example.bnk_project_02s.service.ShoppingService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user/shopping")
@CrossOrigin(origins = "*")
public class ShoppingController {

    @Autowired
    private ShoppingService shoppingService;

    @GetMapping("/product/api")
    @ResponseBody
    public String getProduct(@RequestParam("spno") String spno) {
        try {
            return shoppingService.getProductDetails(spno);
        } catch (Exception e) {
            return "에러 발생: " + e.getMessage();
        }
    }

    @GetMapping("/product/save")
    public String getProduct(@RequestParam("spno") String spno, Model model) {
        try {
            ShoppingProductsDto product = shoppingService.getOrFetchProduct(spno);
            model.addAttribute("product", product);
            return "user/shopping/product";
        } catch (Exception e) {
            ShoppingProductsDto emptyProduct = new ShoppingProductsDto();
            emptyProduct.setSpno(spno);
            emptyProduct.setSpname("제품 로드 실패");
            model.addAttribute("product", emptyProduct);
            model.addAttribute("error", "제품을 가져올 수 없습니다: " + e.getMessage());
            return "user/shopping/product";
        }
    }

    @GetMapping("/products")
    @ResponseBody
    public List<ShoppingProductsDto> getProductList() {
        return shoppingService.getProductList();
    }

    @GetMapping("/products/detail/{spno}")
    @ResponseBody
    public ShoppingProductsDto getProductDetail(@PathVariable("spno") String spno) {
        return shoppingService.getOrFetchProduct(spno);
    }

    @GetMapping("/user/info")
    @ResponseBody
    public Map<String, Object> getUserInfo(HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        User loginUser = (User) session.getAttribute("LOGIN_USER");
        if (loginUser != null) {
            result.put("isLoggedIn", true);
            result.put("uname", loginUser.getUname());
            result.put("uid", loginUser.getUid());
        } else {
            result.put("isLoggedIn", false);
            result.put("uname", "회원");
        }

        return result;
    }

    @GetMapping("/translate/all")
    @ResponseBody
    public String translateAllProducts() {
        try {
            shoppingService.translateExistingProducts();
            return "모든 상품 번역 완료";
        } catch (Exception e) {
            return "번역 실패: " + e.getMessage();
        }
    }
}