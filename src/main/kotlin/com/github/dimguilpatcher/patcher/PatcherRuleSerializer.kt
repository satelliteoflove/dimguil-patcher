package com.github.dimguilpatcher.patcher

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

object PatcherRuleSerializer : KSerializer<PatcherRule> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("PatcherRule") {
        element<String>("file")
        element<Map<String, String>>("edits")
    }

    override fun serialize(
        encoder: Encoder,
        value: PatcherRule
    ) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.file)
            encodeSerializableElement(
                descriptor,
                1,
                MapSerializer(String.serializer(), String.serializer()),
                value.edits
                    .mapKeys { "%x".format(it.key.toInt()) }
                    .mapValues { l ->
                        val sb = StringBuilder()
                        for (byte in l.value) {
                            sb.append("%02x".format(byte))
                        }
                        sb.toString()
                    } )
        }
    }

    override fun deserialize(decoder: Decoder): PatcherRule {
        return decoder.decodeStructure(descriptor) {
            var file : String? = null
            var edits: Map<String, String> = emptyMap()

            loop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    DECODE_DONE -> break@loop

                    0 -> file = decodeStringElement(descriptor, 0)
                    1 -> edits = decodeSerializableElement(descriptor, 1, MapSerializer(String.serializer(), String.serializer()))

                    else -> throw SerializationException("Unexpected index $index")
                }
            }

            PatcherRule(requireNotNull(file), edits
                .mapKeys { it.key.hexToUInt() }
                .mapValues { l ->
                    val string = l.value
                    val bytes = mutableListOf<Byte>()
                    var i = 0
                    while (i < string.length) {
                        if (string[i] != ' ' && string[i + 1] != ' ') {
                            bytes += "${string[i]}${string[i + 1]}".hexToByte()
                            i += 2
                        } else {
                            i += 1
                        }
                    }
                    bytes
                })
        }
    }
}