package com.isg.kyc.ai;

import java.util.List;

public record ChatReply(String content, List<String> toolsUsed) {}