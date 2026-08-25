use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jdouble;

#[no_mangle]
pub extern "system" fun Java_com_mypocket_app_NativeLib_formatCurrency(
    env: JNIEnv,
    _class: JClass,
    amount: jdouble,
) -> JString {
    let formatted = format!("$ {:.2}", amount);
    env.new_string(formatted).expect("Couldn't create java string!")
}
