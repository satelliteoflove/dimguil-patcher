package com.github.dimguilpatcher

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

object SectionConfigDataSerializer : KSerializer<SectionConfigData> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SectionConfigData") {
        element<String>("fileName")
        element<List<String>>("headerAddresses")
    }

    override fun serialize(encoder: Encoder, value: SectionConfigData) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.fileName)
            encodeSerializableElement(
                descriptor,
                1,
                ListSerializer(String.serializer()),
                value.headerAddresses.map { "%x".format(it) })
        }
    }

    override fun deserialize(decoder: Decoder): SectionConfigData {
        return decoder.decodeStructure(descriptor) {
            var fileName: String? = null
            var headerAddresses: List<String> = emptyList()

            loop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    DECODE_DONE -> break@loop

                    0 -> fileName = decodeStringElement(descriptor, 0)
                    1 -> headerAddresses = decodeSerializableElement(descriptor, 1, ListSerializer(String.serializer()))

                    else -> throw SerializationException("Unexpected index $index")
                }
            }

            SectionConfigData(requireNotNull(fileName), headerAddresses.map { it.hexToUInt() })
        }
    }
}
