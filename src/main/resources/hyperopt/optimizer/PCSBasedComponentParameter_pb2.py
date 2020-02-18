# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: PCSBasedComponentParameter.proto

import sys
_b=sys.version_info[0]<3 and (lambda x:x) or (lambda x:x.encode('latin1'))
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()




DESCRIPTOR = _descriptor.FileDescriptor(
  name='PCSBasedComponentParameter.proto',
  package='pcsbasedoptimization',
  syntax='proto3',
  serialized_options=_b('\n(ai.libs.hasco.pcsbasedoptimization.protoP\001\210\001\001'),
  serialized_pb=_b('\n PCSBasedComponentParameter.proto\x12\x14pcsbasedoptimization\"h\n\x16PCSBasedComponentProto\x12\x0c\n\x04name\x18\x01 \x01(\t\x12@\n\nparameters\x18\x02 \x03(\x0b\x32,.pcsbasedoptimization.PCSBasedParameterProto\"4\n\x16PCSBasedParameterProto\x12\x0b\n\x03key\x18\x01 \x01(\t\x12\r\n\x05value\x18\x02 \x01(\t\"1\n\x1fPCSBasedEvaluationResponseProto\x12\x0e\n\x06result\x18\x01 \x01(\x01\x32\x8d\x01\n\x18PCSBasedOptimizerService\x12q\n\x08\x45valuate\x12,.pcsbasedoptimization.PCSBasedComponentProto\x1a\x35.pcsbasedoptimization.PCSBasedEvaluationResponseProto\"\x00\x42/\n(ai.libs.hasco.pcsbasedoptimization.protoP\x01\x88\x01\x01\x62\x06proto3')
)




_PCSBASEDCOMPONENTPROTO = _descriptor.Descriptor(
  name='PCSBasedComponentProto',
  full_name='pcsbasedoptimization.PCSBasedComponentProto',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='name', full_name='pcsbasedoptimization.PCSBasedComponentProto.name', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='parameters', full_name='pcsbasedoptimization.PCSBasedComponentProto.parameters', index=1,
      number=2, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=58,
  serialized_end=162,
)


_PCSBASEDPARAMETERPROTO = _descriptor.Descriptor(
  name='PCSBasedParameterProto',
  full_name='pcsbasedoptimization.PCSBasedParameterProto',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='key', full_name='pcsbasedoptimization.PCSBasedParameterProto.key', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='value', full_name='pcsbasedoptimization.PCSBasedParameterProto.value', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=164,
  serialized_end=216,
)


_PCSBASEDEVALUATIONRESPONSEPROTO = _descriptor.Descriptor(
  name='PCSBasedEvaluationResponseProto',
  full_name='pcsbasedoptimization.PCSBasedEvaluationResponseProto',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='result', full_name='pcsbasedoptimization.PCSBasedEvaluationResponseProto.result', index=0,
      number=1, type=1, cpp_type=5, label=1,
      has_default_value=False, default_value=float(0),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=218,
  serialized_end=267,
)

_PCSBASEDCOMPONENTPROTO.fields_by_name['parameters'].message_type = _PCSBASEDPARAMETERPROTO
DESCRIPTOR.message_types_by_name['PCSBasedComponentProto'] = _PCSBASEDCOMPONENTPROTO
DESCRIPTOR.message_types_by_name['PCSBasedParameterProto'] = _PCSBASEDPARAMETERPROTO
DESCRIPTOR.message_types_by_name['PCSBasedEvaluationResponseProto'] = _PCSBASEDEVALUATIONRESPONSEPROTO
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

PCSBasedComponentProto = _reflection.GeneratedProtocolMessageType('PCSBasedComponentProto', (_message.Message,), {
  'DESCRIPTOR' : _PCSBASEDCOMPONENTPROTO,
  '__module__' : 'PCSBasedComponentParameter_pb2'
  # @@protoc_insertion_point(class_scope:pcsbasedoptimization.PCSBasedComponentProto)
  })
_sym_db.RegisterMessage(PCSBasedComponentProto)

PCSBasedParameterProto = _reflection.GeneratedProtocolMessageType('PCSBasedParameterProto', (_message.Message,), {
  'DESCRIPTOR' : _PCSBASEDPARAMETERPROTO,
  '__module__' : 'PCSBasedComponentParameter_pb2'
  # @@protoc_insertion_point(class_scope:pcsbasedoptimization.PCSBasedParameterProto)
  })
_sym_db.RegisterMessage(PCSBasedParameterProto)

PCSBasedEvaluationResponseProto = _reflection.GeneratedProtocolMessageType('PCSBasedEvaluationResponseProto', (_message.Message,), {
  'DESCRIPTOR' : _PCSBASEDEVALUATIONRESPONSEPROTO,
  '__module__' : 'PCSBasedComponentParameter_pb2'
  # @@protoc_insertion_point(class_scope:pcsbasedoptimization.PCSBasedEvaluationResponseProto)
  })
_sym_db.RegisterMessage(PCSBasedEvaluationResponseProto)


DESCRIPTOR._options = None

_PCSBASEDOPTIMIZERSERVICE = _descriptor.ServiceDescriptor(
  name='PCSBasedOptimizerService',
  full_name='pcsbasedoptimization.PCSBasedOptimizerService',
  file=DESCRIPTOR,
  index=0,
  serialized_options=None,
  serialized_start=270,
  serialized_end=411,
  methods=[
  _descriptor.MethodDescriptor(
    name='Evaluate',
    full_name='pcsbasedoptimization.PCSBasedOptimizerService.Evaluate',
    index=0,
    containing_service=None,
    input_type=_PCSBASEDCOMPONENTPROTO,
    output_type=_PCSBASEDEVALUATIONRESPONSEPROTO,
    serialized_options=None,
  ),
])
_sym_db.RegisterServiceDescriptor(_PCSBASEDOPTIMIZERSERVICE)

DESCRIPTOR.services_by_name['PCSBasedOptimizerService'] = _PCSBASEDOPTIMIZERSERVICE

# @@protoc_insertion_point(module_scope)
