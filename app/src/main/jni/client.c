#include "com_lanan_filetransport_utils_Jni.h"
#include <stdio.h>
#include <stdlib.h>
#include <netinet/in.h>
#include <unistd.h>
#include <netdb.h>
#include <arpa/inet.h>
#include <openssl/err.h>
#include <openssl/ssl.h>
#include <sys/stat.h>
#include <fcntl.h>

#define MAXBUF 1024

enum ERROR_SET {
    SUCCESS, SET_MODE_FAILED, LOAD_CA_FAILED, CONNECTION_FAILED, FILE_NOT_EXISTS,
    GET_RESPONSE_FAILED, OPEN_FILE_FAILED, FAILED
};

SSL *top_ssl;
SSL_CTX *ctx;
int client_fd;

int open_connection(const char *hostname, int port) {
    int sd;
    struct hostent *host;
    struct sockaddr_in addr;
    if ((host = gethostbyname(hostname)) == NULL) {
        return -1;
    }
    sd = socket(PF_INET, SOCK_STREAM, 0);
    bzero(&addr, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);
    addr.sin_addr.s_addr = *(__be32 *) (host->h_addr);
    if (connect(sd, (struct sockaddr *) &addr, sizeof(addr)) != 0) {
        close(sd);
        return -1;
    }
    return sd;
}

int get_file_size(const char *filename) {
    struct stat stat_buf;
    stat(filename, &stat_buf);
    int size = (int) stat_buf.st_size;

    return size;
}

void finish() {
    SSL_free(top_ssl);
    close(client_fd);
    SSL_CTX_free(ctx);
}

JNIEXPORT jint JNICALL Java_com_lanan_filetransport_utils_Jni_client_1send_1file
        (JNIEnv *env, jobject obj, jstring j_serverip, jint j_port, jstring j_capath, jstring j_filepath, jstring j_ip) {
    int len;
    char *filename, *tmp;
    const char head[8] = "hostip=";
    const char mid[11] = ";filename=";
    const char end[13] = ";filelength=";
    const char sep = '/';
    char buf[MAXBUF] = {0};
    char filelength[32] = {0};
    char ip[16] = {0};
    char filepath[256] = {0};
    char *content = (char *)malloc(256);
    bzero(content, 256);

    char serverip[128] = {0};
    int ip_len = (*env)->GetStringLength(env, j_serverip);
    (*env)->GetStringUTFRegion(env, j_serverip, 0, ip_len, serverip);

    int port = j_port;

    char ca_path[128] = {0};
    int path_len = (*env)->GetStringLength(env, j_capath);
    (*env)->GetStringUTFRegion(env, j_capath, 0, path_len, ca_path);

    SSL_library_init();
    SSL_load_error_strings();
    ctx = SSL_CTX_new(SSLv23_client_method());
    if (ctx == NULL) {
        ERR_print_errors_fp(stdout);
        return SET_MODE_FAILED;
    }

    if (!SSL_CTX_load_verify_locations(ctx, ca_path, NULL)) {
        ERR_print_errors_fp(stdout);
        return LOAD_CA_FAILED;
    }

    client_fd = open_connection(serverip, port);
    top_ssl = SSL_new(ctx);
//    SSL_set_cipher_list(top_ssl, "SSL-GP");

    SSL_set_fd(top_ssl, client_fd);
    if (SSL_connect(top_ssl) == -1) {
        return CONNECTION_FAILED;
    } else {
        LOGE("使用%s算法套件\n", SSL_get_cipher(top_ssl));
    }

    int file_len = (*env)->GetStringLength(env, j_filepath);
    (*env)->GetStringUTFRegion(env, j_filepath, 0, file_len, filepath);

    int hostip_len = (*env)->GetStringLength(env, j_ip);
    (*env)->GetStringUTFRegion(env, j_ip, 0, hostip_len, ip);

    if (access(filepath, F_OK) == -1) {
        return FILE_NOT_EXISTS;
    }

    tmp = strrchr(filepath, sep) + 1;
    filename = (char *) malloc(strlen(tmp));
    strncpy(filename, tmp, strlen(filename));
    sprintf(filelength, "%d", get_file_size(filepath));

    strncat(content, head, strlen(head));
    strncat(content, ip, strlen(ip));
    strncat(content, mid, strlen(mid));
    strncat(content, filename, strlen(filename));
    strncat(content, end, strlen(end));
    strncat(content, filelength, strlen(filelength));
    free(filename);

    len = SSL_write(top_ssl, content, strlen(content));
    if (len > 0) {
        free(content);
        len = SSL_read(top_ssl, buf, MAXBUF);
        LOGD("Client: read %d", len);
        if (len <= 0) {
            finish();
            return GET_RESPONSE_FAILED;
        }

        int fd = open(filepath, O_RDWR);
        if (fd != -1) {
            while ((len = read(fd, buf, sizeof(buf))) > 0) {
                SSL_write(top_ssl, buf, len);
                memset(buf, 0, sizeof(buf));
            }
        } else {
            return OPEN_FILE_FAILED;
        }
        close(fd);
        finish();
        return SUCCESS;
    } else {
        finish();
        return FAILED;
    }
}
