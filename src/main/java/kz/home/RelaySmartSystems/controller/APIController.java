package kz.home.RelaySmartSystems.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import kz.home.RelaySmartSystems.model.User;
import kz.home.RelaySmartSystems.model.relaycontroller.RelayController;
import kz.home.RelaySmartSystems.repository.RelayControllerRepository;
import kz.home.RelaySmartSystems.repository.UserRepository;
import kz.home.RelaySmartSystems.service.RelayControllerService;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.KeyFactory;

import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;

@RestController
@RequestMapping(value = "/api")
public class APIController {
    // TODO : временный класс. Для всяких тестов
    private final UserRepository userRepository;
    private final RelayControllerRepository relayControllerRepository;
    private final RelayControllerService relayControllerService;

    //    private final ModelMapper modelMapper;
//    public APIController(UserRepository userRepository,
//                         DeviceRepository deviceRepository,
//                         RelayControllerRepository relayControllerRepository,
//                         ModelMapper modelMapper) {
//        this.userRepository = userRepository;
//        this.deviceRepository = deviceRepository;
//        this.relayControllerRepository = relayControllerRepository;
//        this.modelMapper = modelMapper;
//    }

    private String key = //"-----BEGIN PRIVATE KEY-----\n" +
            "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDMom+M6TzhI91j" +
            "5uHMJoR1A1Bxj16QJYYenCile0QWhGLSRtM/7mEE36c8J+24PZUafGlbWpDB6BZr" +
            "hOTIv8JUNkTYGKi1ce4zJ9VAoL5gfJ8n8uBGQbXX1EPVgH2zCfRQI7+t5xoYdw53" +
            "hQxok6+4l/znM2zkIIg3zlvqggQzNvNG9k28mrJrZmYbr/6dZbZGigqEdPCk2QoC" +
            "MFo/KBSOSvnvjjLFvpDW1ujD8ImWh8/PxiZ6uRaZQQHedu+oSwmnTB+AhTnVZkvY" +
            "5bV7Sd0AEeq/S1+olt8WCZaMdjTZgT2vTVi5Y5V+fZ9DTEjulA7QWU0LMjQymII+" +
            "Tb8gHhdZAgMBAAECggEAGjQSpLCZIxs8ljZSY5nrDDDIdEIvkbW8Xt8QNWB7b4a0" +
            "QMjg9CbKNZ2OBJ34SsdX+HRF6XTHZI0bkdkKkso7UB1C44kE0XctUU+gdw4eUqyi" +
            "lLL9FQIFDwYXSZeSgQKdTkXFnbciGE/mgld/J0UCE1kjAVgMcYnY54x9KKJNd2Ev" +
            "VMw0ZUUB9c35H3UfUwI0eWgmZHsdJJ3HnWc2I5+MK9JQSWmbTP5hxbQxDsO7pONn" +
            "wAt3H7rBp7sIICIA8YOZtv24WsnzRKCMFEoKv9hL/fnqfC4kXpBVfyuYPp83GnyQ" +
            "JW2e9f7fleSZ2fMuvf2TK8C/2j2pIlJsNTBegqJgoQKBgQDpdpPeMmrkI3coB/se" +
            "XV2Qley+us7sL3m+YPvCav02Lw+FG1M8ViEv0onr4UP8htYVZhWdUkdxnfIF0N6S" +
            "Z0CQ50OZGb/0JqLW047fZx12OHNxpM/opYhN6D28v7LmVjvmBImg14HU36dqrqe8" +
            "HZZAE1Lc2l1lJMiH1tyDAnBxzwKBgQDgY28W6FgYoMSRX6g5E9BHA3KYRo1R0/ir" +
            "MnI6RjSi94+Gh8mVWZmDDttNVEBd8TfPS5MINNUr3GqQ0njZm6+W7qmc1nizyfrV" +
            "RRdqJ5qXZZP3FlB/KnK+lfgKS+zc+zpHvlsiaxl3wCSM99B03U5OVPGVrpa4HVKV" +
            "3uIdB+52VwKBgQDjigCANV9czvwZdf3YEGNaweSl1+hI5dSgKmH1kNUNdDyKHKG5" +
            "UrCxrV9jGIBspPYOkRpL5J9hKvFxnarvwdZ55AxMMX6WdPmMq1C6iAN873QEtP81" +
            "3e/FDq7tQWEZgb6LZqzEqIYPdZP0NBmjDKsd9Dd0rRcNtxYC3vS4cy4onQKBgCCr" +
            "mZMWRS1gyv33lYCp99s/D0JIk27klAIpGCSP8D4CGW2W+6y5HPbOBPQfXjfPVTbj" +
            "ZAb/2kHGl+V6H4pfdpNdMGjbeTuEHvdKVfxow8NjUMXbA+FgBtDk+PZW06fhFD05" +
            "4/8A5PZgjXHQ6xL43dvd1kba4qrv6gRULUvYycmFAoGAPueu+2b6LqlZB4XJLetx" +
            "NTHSFDyyrHM/UeqtMA0J1uelpX9JHDa4OXjOzs2+e5DjC0/VNGfCjyp0fWuuk71H" +
            "dA6fLtLIAllGQ0c3soohXDKRHtAEmeiri5MOHvlQieR3dpkRXwRWyh91/llFWGul" +
            "hsEOoRXDPxct0Wbcz94jwjI=";
            //"-----END PRIVATE KEY-----";

    public APIController(UserRepository userRepository,
                         RelayControllerRepository relayControllerRepository, RelayControllerService relayControllerService) {
        this.userRepository = userRepository;
        this.relayControllerRepository = relayControllerRepository;
        this.relayControllerService = relayControllerService;
    }

    @GetMapping("/users")
    List<User> getUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/users/{id}")
    User getUser(@PathVariable String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(String.format("user not found %s", id)));
    }

    @PostMapping("/adduser")
    public User addUser(@RequestBody User newUser) {
        return userRepository.save(newUser);
    }

    @DeleteMapping("/users/{id}")
    void deleteUser(@PathVariable String id) {
        userRepository.deleteById(id);
    }

    @PostMapping("/addrelaycontroller")
    public RelayController addRelayController(@RequestBody RelayController relayController) {
        User user = userRepository.findById("user").orElse(null);
        return relayControllerService.addRelayController(relayController, user);
    }

    @GetMapping(path = "/config/{mac}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getConfig(@PathVariable String mac) {
        return relayControllerService.makeDeviceConfig(mac);
    }

    public PrivateKey generateJwtKeyEncryption(String jwtPrivateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        byte[] keyBytes = Base64.decodeBase64(jwtPrivateKey);
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec=new PKCS8EncodedKeySpec(keyBytes);
        return keyFactory.generatePrivate(pkcs8EncodedKeySpec);
    }

    @GetMapping(path = "/test")
    public String getTest() throws NoSuchAlgorithmException, InvalidKeySpecException {
        return Jwts.builder()
                .setSubject("user123")
                .signWith(SignatureAlgorithm.RS256, generateJwtKeyEncryption(key))
                .compact();
    }

//    private RelayController mapAndSaveRelayController(RelayController relayController) {
//        RelayController newRelayController = new RelayController();
//        modelMapper.map(relayController, newRelayController);
//
//        List<Output> newOutputs = new ArrayList<>();
//        for (Output output : relayController.getOutputs()) {
//            Output newOutput = modelMapper.map(output, Output.class);
//            newOutput.setRelayController(newRelayController);
//            newOutputs.add(newOutput);
//        }
//
//        newRelayController.setOutputs(newOutputs);
//        return newRelayController;
//    }
}
