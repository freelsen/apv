#include "fitz-internal.h"
#include "mupdf-internal.h"


#include <jni.h>
#include "../../pdfview2/apvcore.h"


/* defined in apvandroid.c */
JavaVM *apv_get_cached_jvm();
int get_descriptor_from_file_descriptor(JNIEnv *env, jobject this);


char* apv_get_cmap_data(char *name, int *len, JNIEnv **penv, jbyteArray *pbyteArray) {
	static jni_ids_cached = 0;
	static jmethodID getCmapData_method_id;

	JavaVM *jvm = NULL;
	JNIEnv *jni_env = NULL;
	jclass pdf_class = NULL;

	jstring jname = NULL;

	jbyteArray jbytes = NULL;
	int jbytes_size = 0;
	jbyte *jbytes_internal = NULL;


	jvm = apv_get_cached_jvm();
	(*jvm)->GetEnv(jvm, (void**)&jni_env, JNI_VERSION_1_4);

	pdf_class = (*jni_env)->FindClass(jni_env, "cx/hell/android/lib/pdf/PDF");

	if (!jni_ids_cached) {
		getCmapData_method_id = (*jni_env)->GetStaticMethodID(jni_env, pdf_class, "getCmapData", "(Ljava/lang/String;)[B");
		if (!getCmapData_method_id) {
			APV_LOG_PRINT(APV_LOG_ERROR, "\"getCmapData\" method not found");
			return NULL;
		}
		jni_ids_cached = 1;
	}

	jname = (*jni_env)->NewStringUTF(jni_env, name);
	jbytes = (jbyteArray) (*jni_env)->CallStaticObjectMethod(jni_env, pdf_class, getCmapData_method_id, jname);
	(*jni_env)->DeleteLocalRef(jni_env, jname); /* delete local ref asap even though it's not strictly needed */

	if (!jbytes) {
		len = 0;
		penv = NULL;
		pbyteArray = NULL;
		return NULL;
	}

	jbytes_size = (*jni_env)->GetArrayLength(jni_env, jbytes);
	jbytes_internal = (*jni_env)->GetByteArrayElements(jni_env, jbytes, NULL);

	/* return  many values */
	*penv = jni_env;
	*pbyteArray = jbytes;
	*len = jbytes_size;
	return jbytes_internal;
}



void apv_release_cmap_data(JNIEnv *jni_env, jbyteArray jbytes, jbyte *jbytes_internal) {
	(*jni_env)->ReleaseByteArrayElements(jni_env, jbytes, jbytes_internal, JNI_ABORT);
}


pdf_cmap *pdf_load_builtin_cmap(fz_context *ctx, char *cmap_name) {
	JNIEnv *jni_env = NULL;
	jbyteArray jbytes = NULL;
	jbyte *buf = NULL;
	fz_stream *fi = NULL;
	pdf_cmap *cmap = NULL;
	int buf_len = 0;

	buf = apv_get_cmap_data(cmap_name, &buf_len, &jni_env, &jbytes);
	if (buf) {
		fi = fz_open_memory(ctx, buf, buf_len);
		cmap = pdf_load_cmap(ctx, fi);
		fz_close(fi);
		apv_release_cmap_data(jni_env, jbytes, buf);
		return cmap;
	} else {
		return NULL;
	}
}
