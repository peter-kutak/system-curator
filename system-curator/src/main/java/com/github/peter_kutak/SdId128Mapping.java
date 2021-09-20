package com.github.peter_kutak;

import com.sun.jna.LastErrorException;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.NativeLongByReference;
import com.sun.jna.ptr.PointerByReference;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
 
/**
 *
 */
public interface SdId128Mapping extends Library {

  SdId128Mapping INSTANCE = Native.load("systemd", SdId128Mapping.class);

  @FieldOrder({"bytes"})
  public static class sd_id128 extends Structure{
    public static class ByValue extends sd_id128 implements Structure.ByValue {
      public ByValue(String data) { super(data); }
    }

    public byte bytes[];

    public sd_id128() {
      super();
      this.bytes = new byte[16];
    }

    public sd_id128(String data) {
      super();
      this.init(data);
    }

    public void init(String data) {
      try {
        this.bytes = Hex.decodeHex(data.replace("-", ""));
      } catch (DecoderException e) {
        //toto sa nestane
      }
    }

    public String toString() {
      return String.format(SdId128Mapping.SD_ID128_UUID_FORMAT_STR
          , bytes[0],  bytes[1],  bytes[2],  bytes[3]
          , bytes[4],  bytes[5],  bytes[6],  bytes[7]
          , bytes[8],  bytes[9],  bytes[10], bytes[11]
          , bytes[12], bytes[13], bytes[14], bytes[15]
          );
    }
  }


  int sd_id128_get_machine(sd_id128 ret);
  int sd_id128_get_boot(sd_id128 ret);
  int sd_id128_get_invocation(sd_id128 ret);


  int sd_id128_get_boot_app_specific(sd_id128.ByValue app_id, sd_id128 ret);
  int sd_id128_get_machine_app_specific(sd_id128.ByValue app_id, sd_id128 ret);
  
  public static final String SD_ID128_FORMAT_STR = "%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x";
  public static final String SD_ID128_UUID_FORMAT_STR = "%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x";
  public static final sd_id128 SD_ID128_NULL = new sd_id128("00000000-0000-0000-0000-000000000000");
  public static final sd_id128 SD_ID128_ALLF = new sd_id128("FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF");

}

