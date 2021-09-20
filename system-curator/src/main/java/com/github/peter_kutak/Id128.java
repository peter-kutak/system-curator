package com.github.peter_kutak;

import com.github.peter_kutak.SdId128Mapping.sd_id128;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.NativeLongByReference;
import com.sun.jna.ptr.PointerByReference;

import com.sun.jna.platform.linux.LibC;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * SystemD IDs.
 *
 */
public class Id128 {

  static final SdId128Mapping sd = SdId128Mapping.INSTANCE;

  /**
   *
   */
  public static String get_boot_id() {
    SdId128Mapping.sd_id128 id = new SdId128Mapping.sd_id128();
    sd.sd_id128_get_boot(id);
    return id.toString();
  }

  /**
   *
   */
  public static String get_boot_app_specific(String app_id) {
    SdId128Mapping.sd_id128 id = new SdId128Mapping.sd_id128();
    sd.sd_id128_get_boot_app_specific(new SdId128Mapping.sd_id128.ByValue(app_id), id);
    return id.toString();
  }

  /**
   *
   */
  public static String get_machine_id() {
    SdId128Mapping.sd_id128 id = new SdId128Mapping.sd_id128();
    SdId128Mapping.INSTANCE.sd_id128_get_machine(id);
    return id.toString();
  }

  /**
   *
   */
  public static String get_machine_app_specific(String app_id) {
    SdId128Mapping.sd_id128 id = new SdId128Mapping.sd_id128();
    SdId128Mapping.INSTANCE.sd_id128_get_machine_app_specific(new SdId128Mapping.sd_id128.ByValue(app_id), id);
    return id.toString();
  }

  static void version4variant1(sd_id128 id) {
    id.bytes[6] = (byte) (id.bytes[6] & 0x0f | 0x40);
    id.bytes[8] = (byte) (id.bytes[8] & 0x3f | 0x80);
  }

  public static sd_id128 hmac256(String id, String app_id) {
    byte[] key = new sd_id128(id).bytes;
    byte[] value = new sd_id128(app_id).bytes;

    HmacUtils hm256 = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, key);
    //hm256 object can be used again and again
    String hmac = hm256.hmacHex(value);
    System.out.println(hmac);
    sd_id128 result = new sd_id128(hmac.substring(0,32));
    version4variant1(result);
    return result;
  }
}

