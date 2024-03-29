package com.codesvenue.counterclinic.setting.service;

import com.codesvenue.counterclinic.setting.model.Setting;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface SettingService {
    List<Setting> getAllSettings();

    Setting createOrUpdateSetting(Setting settingInfo);

    Boolean deleteSetting(Integer settingId);

    Setting getSetting(int settingId);

    Setting getSetting(String settingName);
}
