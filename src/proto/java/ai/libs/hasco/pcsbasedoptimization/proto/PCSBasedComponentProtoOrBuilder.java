// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: PCSBasedComponentParameter.proto

package ai.libs.hasco.pcsbasedoptimization.proto;

public interface PCSBasedComponentProtoOrBuilder extends
		// @@protoc_insertion_point(interface_extends:pcsbasedoptimization.PCSBasedComponentProto)
		com.google.protobuf.MessageOrBuilder {

	/**
	 * <code>string name = 1;</code>
	 */
	java.lang.String getName();

	/**
	 * <code>string name = 1;</code>
	 */
	com.google.protobuf.ByteString getNameBytes();

	/**
	 * <code>repeated .pcsbasedoptimization.PCSBasedParameterProto parameters = 2;</code>
	 */
	java.util.List<ai.libs.hasco.pcsbasedoptimization.proto.PCSBasedParameterProto> getParametersList();

	/**
	 * <code>repeated .pcsbasedoptimization.PCSBasedParameterProto parameters = 2;</code>
	 */
	ai.libs.hasco.pcsbasedoptimization.proto.PCSBasedParameterProto getParameters(int index);

	/**
	 * <code>repeated .pcsbasedoptimization.PCSBasedParameterProto parameters = 2;</code>
	 */
	int getParametersCount();

	/**
	 * <code>repeated .pcsbasedoptimization.PCSBasedParameterProto parameters = 2;</code>
	 */
	java.util.List<? extends ai.libs.hasco.pcsbasedoptimization.proto.PCSBasedParameterProtoOrBuilder> getParametersOrBuilderList();

	/**
	 * <code>repeated .pcsbasedoptimization.PCSBasedParameterProto parameters = 2;</code>
	 */
	ai.libs.hasco.pcsbasedoptimization.proto.PCSBasedParameterProtoOrBuilder getParametersOrBuilder(int index);
}
