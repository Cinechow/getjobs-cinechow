package utils;

import lombok.Data;

import java.io.Serializable;

@Data
public class Job implements Serializable {
//    岗位链接
    private String href;
//    岗位名称
    private String jobName;
//    岗位地区
    private String jobArea;
//    岗位信息
    private String jobInfo;
//    岗位薪水
    private String salary;
//    公司标签
    private String companyTag;
//    HR名称
    private String recruiter;
//    公司名称
    private String companyName;
//    公司信息
    private String companyInfo;
    @Override
    public String toString() {
        return String.format("【%s, %s, %s, %s, %s, %s】", companyName, jobName, jobArea, salary, companyTag,recruiter);

    }
    public String toString(Platform platform) {
        if (platform == Platform.ZHILIAN){
            return String.format("【%s, %s, %s, %s, %s, %s, %s】", companyName, jobName, jobArea, companyTag, salary, recruiter, href);

        }if (platform == Platform.BOSS) {
            return String.format("【%s, %s, %s, %s, %s, %s】", companyName, jobName, jobArea, salary, companyTag, recruiter);
        }
        return String.format("【%s, %s, %s, %s, %s, %s, %s】", companyName, jobName, jobArea, salary, companyTag, recruiter, href);


    }


}
