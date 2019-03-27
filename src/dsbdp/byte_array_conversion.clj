;;;
;;;   Copyright 2015 Ruediger Gad
;;;
;;;   This software is released under the terms of the Eclipse Public License 
;;;   (EPL) 1.0. You can find a copy of the EPL at: 
;;;   http://opensource.org/licenses/eclipse-1.0.php
;;;

(ns
  ^{:author "Ruediger Gad",
    :doc "Helper functions for converting byte arrays into other data types/representations"}
  dsbdp.byte-array-conversion
  (:import
    (dsbdp ByteArrayHelper)
    (java.text SimpleDateFormat)
    (java.util Date)
    (org.apache.commons.net.ntp TimeStamp)))

(defn int4l
  "Get the lower 4 bits (nibble) of the byte at the given index idx in the provided byte-array ba."
  [ba idx]
  (ByteArrayHelper/getInt4L ba idx))

(defn int4h
  "Get the higher 4 bits (nibble) of the byte at the given index idx in the provided byte-array ba."
  [ba idx]
  (ByteArrayHelper/getInt4H ba idx))

(defn int8
  "Get the byte at the index idx in the byte-array ba."
  [ba idx]
  (ByteArrayHelper/getInt8 ba idx))

(defn int16
  "Get the Int16 value of the two bytes starting at index idx in the byte-array ba."
  [ba idx]
  (ByteArrayHelper/getInt16 ba idx))

(defn int16be
  "Get the big endian Int16 value of the two bytes starting at index idx in the byte-array ba."
  [ba idx]
  (ByteArrayHelper/getInt16BigEndian ba idx))

(defn int32
  "Get the Int32 value of the four bytes starting at index idx in the byte-array ba."
  [ba idx]
  (ByteArrayHelper/getInt32 ba idx))

(defn int32be
  "Get the big endian Int32 value of the four bytes starting at index idx in the byte-array ba."
  [ba idx]
  (ByteArrayHelper/getInt32BigEndian ba idx))

(defn int64
  "Get the Int64 value of the four bytes starting at index idx in the byte-array ba."
  [ba idx]
  (ByteArrayHelper/getInt64 ba idx))

(defn int64be
  "Get the big endian Int32 value of the four bytes starting at index idx in the byte-array ba."
  [ba idx]
  (ByteArrayHelper/getInt64BigEndian ba idx))

(defn timestamp
  "Get the (pcap) timestamp long value starting at index idx in the byte-array ba."
  [ba idx]
  (+ (* (ByteArrayHelper/getInt32 ba idx) 1000000000) (* (ByteArrayHelper/getInt32 ba (+ idx 4)) 1000)))

(defn timestamp-be
  "Get the (pcap) big endian timestamp long value starting at index idx in the byte-array ba."
  [ba idx]
  (+ (* (ByteArrayHelper/getInt32BigEndian ba idx) 1000000000) (* (ByteArrayHelper/getInt32BigEndian ba (+ idx 4)) 1000)))

(defn eth-mac-addr-str
  "Starting at index idx of the byte array ba, convert the following bytes into the Ethernet MAC address String representation."
  [ba idx]
  (ByteArrayHelper/getEthMacAddrString ba idx))

(defn ipv4-addr-str
  "Starting at index idx of the byte array ba, convert the following bytes into the IPv4 address String representation."
  [ba idx]
  (ByteArrayHelper/getIpv4AddrString ba idx))

(defn timestamp-to-str
  "Convert a timestamp from its Long value to a formatted String."
  [ts]
  (->
    (SimpleDateFormat. "yyyy-MM-DD_HH:mm:ss")
    (.format
      (Date.
        (long (/ ts 1000000))))))

(defn timestamp-str
  "Get the (pcap) timestamp String representation starting at index idx in the byte-array ba."
  [ba idx]
  (timestamp-to-str (timestamp ba idx)))

(defn timestamp-str-be
  "Get the (pcap) big endian timestamp String representation starting at index idx in the byte-array ba."
  [ba idx]
  (timestamp-to-str (timestamp-be ba idx)))

(defn ba-to-str
  "Convert a byte array ba into a String representation, interpreting the ba content as ASCII values.
   Starts at index idx and converts the number of len bytes."
  [ba idx len]
  (ByteArrayHelper/byteArrayToString ba idx len))

(defn ntp-timestamp-str
  "Get the NTP timestamp String representation starting at index idx in the byte-array ba."
  [ba idx]
  (-> (TimeStamp. (int64 ba idx)) (.toDateString)))

(defn ntp-timestamp-str-be
  "Get the NTP big endian timestamp String representation starting at index idx in the byte-array ba."
  [ba idx]
  (-> (TimeStamp. (int64be ba idx)) (.toDateString)))

(defn int-to-byte
  [x]
  (if
    (> x 127)
    (+ -128 (- x 128))
    x))

