package com.hierynomus.msfscc;

import com.hierynomus.protocol.commons.EnumWithValue;

public enum FileInformationClass implements EnumWithValue<FileInformationClass> {
  FileDirectoryInformation(1L),
  FileFullDirectoryInformation(2L),
  FileBothDirectoryInformation(3L),
  FileBasicInformation(4L),
  FileStandardInformation(5L),
  FileInternalInformation(6L),
  FileEaInformation(7L),
  FileAccessInformation(8L),
  FileNameInformation(9L),
  FileRenameInformation(10L),
  FileLinkInformation(11L),
  FileNamesInformation(12L),
  FileDispositionInformation(13L),
  FilePositionInformation(14L),
  FileFullEaInformation(15L),
  FileModeInformation(16L),
  FileAlignmentInformation(17L),
  FileAllInformation(18L),
  FileAllocationInformation(19L),
  FileEndOfFileInformation(20L),
  FileAlternateNameInformation(21L),
  FileStreamInformation(22L),
  FilePipeInformation(23L),
  FilePipeLocalInformation(24L),
  FilePipeRemoteInformation(25L),
  FileMailslotQueryInformation(26L),
  FileMailslotSetInformation(27L),
  FileCompressionInformation(28L),
  FileObjectIdInformation(29L),
  FileCompletionInformation(30L),
  FileMoveClusterInformation(31L),
  FileQuotaInformation(32L),
  FileReparsePointInformation(33L),
  FileNetworkOpenInformation(34L),
  FileAttributeTagInformation(35L),
  FileTrackingInformation(36L),
  FileIdBothDirectoryInformation(37L),
  FileIdFullDirectoryInformation(38L),
  FileValidDataLengthInformation(39L),
  FileShortNameInformation(40L),
  FileIoCompletionNotificationInformation(41L),
  FileIoStatusBlockRangeInformation(42L),
  FileIoPriorityHintInformation(43L),
  FileSfioReserveInformationv(44L),
  FileSfioVolumeInformation(45L),
  FileHardLinkInformation(46L),
  FileProcessIdsUsingFileInformation(47L),
  FileNormalizedNameInformation(48L),
  FileNetworkPhysicalNameInformation(49L),
  FileIdGlobalTxDirectoryInformation(50L),
  FileIsRemoteDeviceInformation(51L),
  FileUnusedInformation(52L),
  FileNumaNodeInformation(53L),
  FileStandardLinkInformation(54L),
  FileRemoteProtocolInformation(55L),
  FileRenameInformationBypassAccessCheck(56L),
  FileLinkInformationBypassAccessCheck(57L),
  FileVolumeNameInformation(58L),
  FileIdInformation(59L),
  FileIdExtdDirectoryInformation(60L),
  FileReplaceCompletionInformation(61L),
  FileHardLinkFullIdInformation(62L),
  FileIdExtdBothDirectoryInformation(63L),
  FileMaximumInformation(64L);
  
  private long value;
  
  FileInformationClass(long value) {
    this.value = value;
  }
  
  public long getValue() {
    return this.value;
  }
}
