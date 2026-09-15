/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.misc;

import buildcraft.lib.config.ConfigCategory;
import buildcraft.lib.config.Configuration;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.stream.Collectors;

public class ConfigUtil {
    /** Sets a good default language key for all of the properties contained in the given configuration */
    public static void setLang(Configuration cfg) {
        for (ConfigCategory<?> cat : cfg.getAll()) {
            String catPath = cat.getFullPath();
            while (!"".equals(catPath)) {
                cat.setLanguageKey("config." + catPath);
                String[] splitArray = catPath.split("\\.");
                List<String> splitList = Lists.newArrayList(catPath.split("\\."));
                splitList.remove(splitArray.length - 1);
                catPath = splitList.stream().collect(Collectors.joining("."));
            }
        }
    }




}
