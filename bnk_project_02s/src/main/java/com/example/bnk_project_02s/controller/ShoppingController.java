package com.example.bnk_project_02s.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.bnk_project_02s.dto.ShoppingProductDto;
import com.example.bnk_project_02s.service.ShoppingService;

@Controller
@RequestMapping("/shopping")
@CrossOrigin(origins = "*")
public class ShoppingController {

    @Autowired
    private ShoppingService shoppingService;

    @GetMapping("/product/api")
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
            ShoppingProductDto product = shoppingService.getOrFetchProduct(spno);
            model.addAttribute("product", product);
            return "user/shopping/product";
        } catch (Exception e) {
            ShoppingProductDto emptyProduct = new ShoppingProductDto();
            emptyProduct.setSpno(spno);
            emptyProduct.setSpname("제품 로드 실패");
            model.addAttribute("product", emptyProduct);
            model.addAttribute("error", "제품을 가져올 수 없습니다: " + e.getMessage());
            return "user/shopping/product";
        }
    }
    
    @GetMapping("/products")
    @ResponseBody
    public List<ShoppingProductDto> getProductList() {
        return shoppingService.getProductList();
    }
    
    @GetMapping("/products/detail/{spno}")
    @ResponseBody
    public List<ShoppingProductDto> getProductDetail(@PathVariable("spno") String spno) {
    	return shoppingService.getProductDetail();
    }
}