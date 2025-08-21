package com.github.dimguilpatcher

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

object HeaderDataSerializer : KSerializer<HeaderData> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("HeaderData") {
        element<String>("address")
        element<String>("offset")
    }

    override fun serialize(encoder: Encoder, value: HeaderData) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(
                descriptor,
                0,
                "%x".format(value.address))
            encodeStringElement(
                descriptor,
                1,
                "%x".format(value.offset))
        }
    }

    override fun deserialize(decoder: Decoder): HeaderData {
        return decoder.decodeStructure(descriptor) {
            var address: String? = null
            var offset: String? = null

            loop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    DECODE_DONE -> break@loop

                    0 -> address = decodeStringElement(descriptor, 0)
                    1 -> offset = decodeStringElement(descriptor, 1)

                    else -> throw SerializationException("Unexpected index $index")
                }
            }

            HeaderData(requireNotNull(address).hexToUInt(), requireNotNull(offset).hexToUInt())
        }
    }
}
