package com.kamsiob.steadyhealth.data.entity

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.Base64

/**
 * A photographed page, written into the backup as text.
 *
 * Bytes have no shape of their own in JSON. Left alone, they are written as a
 * list of numbers, which spends four characters on every byte and makes the
 * backup several times the size of the pictures inside it. A page image is the
 * largest thing this app stores, so it is written the way every other tool writes
 * bytes, and a person who opens the file sees one long word rather than a page of
 * digits.
 */
object Base64Bytes : KSerializer<ByteArray> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("PageImage", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ByteArray) {
        encoder.encodeString(Base64.getEncoder().encodeToString(value))
    }

    override fun deserialize(decoder: Decoder): ByteArray =
        Base64.getDecoder().decode(decoder.decodeString())
}
