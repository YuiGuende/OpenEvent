package com.group02.openevent.controller.wallet;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class KycPageController {
    @GetMapping("/kyc-verification")
    public String kycVerification(){
        return "kyc/kyc-verification";
    }
}
