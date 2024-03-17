package com.xuecheng.base.Exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

//响应用户的统一类型
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestErrorResponse implements Serializable {
    private String errMessage;
}
