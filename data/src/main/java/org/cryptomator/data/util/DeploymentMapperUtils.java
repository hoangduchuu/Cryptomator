package org.cryptomator.data.util;


import java.util.regex.Pattern;


public class DeploymentMapperUtils {

	public static String extractEmailFromCloudPath(String cloudPath) {
		if (cloudPath == null || cloudPath.isEmpty()) {
			return "";
		}

		// Match pattern: protocol://email@domain.com@/path
		Pattern pattern = Pattern.compile("[a-z_]+://([^@]+@[^@]+)@");
		java.util.regex.Matcher matcher = pattern.matcher(cloudPath.toLowerCase());

		if (matcher.find()) {
			return matcher.group(1);
		}
		return "";
	}

}
