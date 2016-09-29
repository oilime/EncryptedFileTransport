#include "com_lanan_filetransport_utils_Jni.h"
#include <stdio.h>
#include <stdlib.h>
#include <errno.h>
#include <netinet/in.h>
#include <unistd.h>
#include <sys/stat.h>
#include <arpa/inet.h>
#include <openssl/err.h>
#include <openssl/ssl.h>
#include <fcntl.h>

#define MAXBUF 1024

enum ERROR_SET {
    FAILED, CREATE_CTX_FAILED, LOAD_CA_FAILED, LOAD_SERVER_FAILED, LOAD_SERVER_KEY_FAILED,
    VERIFY_SERVER_FAILED, CREATE_SOCKET_FAILED, SOCKET_BIND_FAILED, SOCKET_LISTEN_FAILED,
    CREATE_DIR_FAILED, SUCCESS
};

volatile int stop;

void ShowCerts(SSL * ssl) {
    X509 *cert = SSL_get_peer_certificate(ssl);
    if (cert != NULL) {
        char *line = NULL;
        if (X509_V_OK != SSL_get_verify_result(ssl)) {
            LOGD("Server: SSL_get_verify_result fail\n");
        }
        LOGD("Server: Digital certificate information:\n");

        line = X509_NAME_oneline(X509_get_subject_name(cert), 0, 0);
        LOGD("Server:   Certificate: %s\n", line);
        free(line);

        line = X509_NAME_oneline(X509_get_issuer_name(cert), 0, 0);
        LOGD("Server:   Issuer: %s\n", line);
        free(line);

        X509_free(cert);
    } else {
        LOGD("Server: No certificate information！\n");
    }
}

void recv_file(JNIEnv *env, jobject obj, const char *filename, const char *filepath, const char *ip) {
    jclass clazz = (*env)->FindClass(env, "com/lanan/filetransport/utils/Jni");
    if (clazz == 0) {
        LOGE("find class error");
        return;
    }

    LOGD("Server: find class");
    jmethodID method1 = (*env)->GetMethodID(
            env, clazz, "newFileRecv", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    if(method1 == 0){
        LOGD("Server: find method error");
        return;
    }

    LOGD("Server: find method");
    (*env)->CallVoidMethod(
            env, obj, method1,
            (*env)->NewStringUTF(env, filename),
            (*env)->NewStringUTF(env, filepath),
            (*env)->NewStringUTF(env, ip));
    LOGD("Server: method called");
}

JNIEXPORT void JNICALL Java_com_lanan_filetransport_utils_Jni_set_1stop
        (JNIEnv *env, jobject obj) {
    stop = 0;
}

JNIEXPORT jint JNICALL Java_com_lanan_filetransport_utils_Jni_server_1set_1socket
        (JNIEnv *env, jobject obj, jint j_port, jstring j_cert, jstring j_ca) {
    int listen_socket;
    int mid_socket;
    int len;
    int flag;
    struct sockaddr_in local_addr;
    struct sockaddr_in client_addr;
    unsigned int port = (unsigned int) j_port;
    char server_buf[MAXBUF];
    char *content, *ip = NULL, *filename = NULL, *filelength = NULL;
    char filepath[1024];
    const char sep = '=';
    struct timeval timeout = {5, 0};
    SSL_CTX *ctx;

    char cert[1024] = {0}, ca[1024] = {0};

    int cert_len = (*env)->GetStringLength(env, j_cert);
    (*env)->GetStringUTFRegion(env, j_cert, 0, cert_len, cert);

    int ca_len = (*env)->GetStringLength(env, j_ca);
    (*env)->GetStringUTFRegion(env, j_ca, 0, ca_len, ca);

    char server_key[128] = "/sdcard/rsacert/server.key";
    char root_dir[128] = "/sdcard/alan/system/security/local/tmp/chs/FileTransport/temp/recv/";

    SSL_library_init();
    SSL_load_error_strings();
    ctx = SSL_CTX_new(SSLv23_server_method());
    if (ctx == NULL) {
        ERR_print_errors_fp(stdout);
        return CREATE_CTX_FAILED;
    }

    if (!SSL_CTX_load_verify_locations(ctx, ca, NULL)) {
        ERR_print_errors_fp(stdout);
        return LOAD_CA_FAILED;
    }

    if (SSL_CTX_use_certificate_file(ctx, cert, SSL_FILETYPE_PEM) <= 0) {
        ERR_print_errors_fp(stdout);
        return LOAD_SERVER_FAILED;
    }

    if (SSL_CTX_use_PrivateKey_file(ctx, server_key, SSL_FILETYPE_PEM) <= 0) {
        ERR_print_errors_fp(stdout);
        return LOAD_SERVER_KEY_FAILED;
    }

    if (!SSL_CTX_check_private_key(ctx)) {
        ERR_print_errors_fp(stdout);
        return VERIFY_SERVER_FAILED;
    }

    if ((listen_socket = socket(AF_INET, SOCK_STREAM, 0)) == -1) {
        return CREATE_SOCKET_FAILED;
    }
    setsockopt(listen_socket, SOL_SOCKET, SO_SNDTIMEO, (char *) &timeout, sizeof(struct timeval));
    setsockopt(listen_socket, SOL_SOCKET, SO_RCVTIMEO, (char *) &timeout, sizeof(struct timeval));


    memset(&local_addr, 0, sizeof(local_addr));
    local_addr.sin_family = AF_INET;
    local_addr.sin_port = htons(port);
    local_addr.sin_addr.s_addr = INADDR_ANY;

    if (bind(listen_socket, (struct sockaddr *) &local_addr, sizeof(struct sockaddr)) == -1) {
        return SOCKET_BIND_FAILED;
    }

    if (listen(listen_socket, 10) == -1) {
        LOGE("监听失败");
        return SOCKET_LISTEN_FAILED;
    }

    stop = 1;
    while (stop) {
        SSL *ssl;
        len = sizeof(struct sockaddr);
        if ((mid_socket = accept(listen_socket, (struct sockaddr *) &client_addr, &len)) == -1) {
            continue;
        }

        ssl = SSL_new(ctx);

        SSL_set_fd(ssl, mid_socket);
        if (SSL_accept(ssl) == -1) {
            perror("accept");
            close(mid_socket);
            break;
        }

        LOGD("Server: 采用的算法套件: %s\n", SSL_get_cipher(ssl));
        ShowCerts(ssl);
        bzero(server_buf, MAXBUF + 1);
        if ((len = SSL_read(ssl, server_buf, MAXBUF)) != EOF) {
            content = strtok(server_buf, ";");
            flag = 0;
            while (content != NULL) {
                switch (flag) {
                    case 0:
                        ip = (char *)malloc(strlen(strchr(content, sep) + 1));
                        strcpy(ip, strchr(content, sep) + 1);
                        LOGD("Server: 客户端ip地址为%s", ip);
                        break;
                    case 1:
                        filename = (char *)malloc(strlen(strchr(content, sep) + 1));
                        strcpy(filename, strchr(content, sep) + 1);
                        LOGD("Server: 文件名为%s", filename);
                        break;
                    case 2:
                        filelength = (char *)malloc(strlen(strchr(content, sep) + 1));
                        strcpy(filelength, strchr(content, sep) + 1);
                        LOGD("Server: 文件长度为%s", filelength);
                    default:
                        break;
                }
                flag++;
                content = strtok(NULL, ";");
            }
        }

        if (access(root_dir, F_OK) == -1) {
            if (mkdir(root_dir, S_IRWXU | S_IRWXG | S_IROTH | S_IXOTH) == -1) {
                return CREATE_DIR_FAILED;
            }
        }

        memset(filepath, 0, sizeof(filepath));
        strncpy(filepath, root_dir, strlen(root_dir));
        strncat(filepath, filename, strlen(filename));

        char test[128] = "ok\0";
        len = SSL_write(ssl, test, strlen(test));
        if (len <= 0) {
            LOGE("消息'%s'发送失败, 错误原因为：%s\n", server_buf, strerror(errno));
            goto finish;
        }

        memset(server_buf, 0, MAXBUF);
        int fd = open(filepath, O_RDWR | O_CREAT | O_EXCL | O_TRUNC);
        if (fd != -1) {
            int j = 0;
            int total = 0;
            len = -1;
            while ((len = SSL_read(ssl, server_buf, MAXBUF)) > 0) {
                write(fd, server_buf, (size_t)len);
                LOGD("Server: read %d with %d", j++, len);
                total += len;
                if (total == atoi(filelength))
                    break;
                memset(server_buf, 0, MAXBUF);
            }
            recv_file(env, obj, filename, filepath, ip);
        }
        close(fd);
        free(ip);
        ip = NULL;
        free(filename);
        filename = NULL;
        free(filelength);
        filelength = NULL;

        finish:
        SSL_shutdown(ssl);
        SSL_free(ssl);
        close(mid_socket);
    }
    close(listen_socket);
    SSL_CTX_free(ctx);
    LOGD("Server: 关闭成功");
    return SUCCESS;
}
