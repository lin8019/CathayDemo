package com.example.demo.encryption;

import com.example.demo.util.EncryptionUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/encryption")
public class EncryptionController {
    @GetMapping("/encrypt")
    public String encryptData(@RequestParam String data) {
        try {
            String encryptedData = EncryptionUtil.encrypt(data);
            return "Encrypted Data: " + encryptedData;
        } catch (Exception e) {
            return "Error encrypting data: " + e.getMessage();
        }
    }

    @GetMapping("/decrypt")
    public String decryptData(@RequestParam String data) {
        try {
            String decryptedData = EncryptionUtil.decrypt(data);
            return "Decrypted Data: " + decryptedData;
        } catch (Exception e) {
            return "Error decrypting data: " + e.getMessage();
        }
    }
}
