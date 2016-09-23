package com.example;

import org.suntongo.gm.provider.SMProvider;

import java.io.FileInputStream;
import java.security.Provider;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class Ecc {
    public static void main(String[] args) throws Exception {
        addProvider();
        CertificateFactory factory = CertificateFactory.getInstance("SM2");
        FileInputStream in = new FileInputStream("/home/emilio/gmssl/rootca/rootca.pem");
        X509Certificate cert = (X509Certificate) factory.generateCertificate(in);

//        CertificateFactory cf = CertificateFactory.getInstance("SM2");
//        FileInputStream in = new FileInputStream("/home/emilio/gmssl/server/server.pfx");
//        KeyStore keyStore = KeyStore.getInstance("PKCS12");
//        keyStore.load(in, "123456".toCharArray());





//
//        PublicKey publicKey = cert.getPublicKey();
//        System.out.println(publicKey.getAlgorithm());
//
//        KeyPair keyPair = new KeyPair()


//        byte[] plainText = "Hello World!".getBytes();
//        byte[] cipherText = null;
//
//        Security.addProvider(new BouncyCastleProvider());
//        //生成公钥和私钥
//        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECIES", "BC");
//        KeyPair keyPair = keyPairGenerator.generateKeyPair();
//        ECPublicKey ecPublicKey = (ECPublicKey) keyPair.getPublic();
//        ECPrivateKey ecPrivateKey = (ECPrivateKey) keyPair.getPrivate();
//        //打印密钥信息
//        ECCurve ecCurve = ecPublicKey.getParameters().getCurve();
//        System.out.println("椭圆曲线参数a = " + ecCurve.getA().toBigInteger());
//        System.out.println("椭圆曲线参数b = " + ecCurve.getB().toBigInteger());
//        System.out.println("椭圆曲线参数q = " + ((ECCurve.Fp) ecCurve).getQ());
//        ECPoint basePoint = ecPublicKey.getParameters().getG();
//        System.out.println("基点橫坐标              "
//                + basePoint.getAffineXCoord().toBigInteger());
//        System.out.println("基点纵坐标              "
//                + basePoint.getAffineYCoord().toBigInteger());
//        System.out.println("公钥横坐标              "
//                + ecPublicKey.getQ().getAffineXCoord().toBigInteger());
//        System.out.println("公钥纵坐标              "
//                + ecPublicKey.getQ().getAffineYCoord().toBigInteger());
//        System.out.println("私钥                         " + ecPrivateKey.getD());
//        Cipher cipher = Cipher.getInstance("ECIESwithDESede/NONE/PKCS7Padding", "BC");
//        // 加密
//        cipher.init(Cipher.ENCRYPT_MODE, ecPublicKey);
//        cipherText = cipher.doFinal(plainText);
//        System.out.println("密文: " + new HexBinaryAdapter().marshal(cipherText));
//        // 解密
//        cipher.init(Cipher.DECRYPT_MODE, ecPrivateKey);
//        plainText = cipher.doFinal(cipherText);
//        // 打印解密后的明文
//        System.out.println("解密后的明文: " + new String(plainText));
    }

    public static void addProvider() {
        Provider prvdr = Security.getProvider("SM");
        if (prvdr == null) {
            prvdr = new SMProvider();
            Security.addProvider(prvdr);
        }
    }
}
