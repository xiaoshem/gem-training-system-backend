package cn.org.alan.exam.utils;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.utils.security.SysUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Security工具类
 *
 * @Author WeiJin
 * @Version 1.0
 * @Date 2024/3/30 0:10
 */
@Slf4j
public class SecurityUtil {

    /**
     * 获取当前用户id
     *
     * @return 用户id
     */
    public static Integer getUserId() {
        return getCurrentUser().getUser().getId();
    }

    /**
     * 获取当前用户角色
     *
     * @return 角色
     */
    public static String getRole() {
        return getAuthorities().get(0).getAuthority();
    }

    /**
     * 获取当前用户角色代码 1：学员、2：培训讲师、3：系统管理员、4：认证审核员
     *
     * @return 角色
     */
    public static Integer getRoleCode() {
        String roleName = getRole();
        Integer roleCode;
        if ("role_admin".equals(roleName)) {
            roleCode = 3;
        } else if ("role_teacher".equals(roleName)) {
            roleCode = 2;
        } else if ("role_student".equals(roleName)) {
            roleCode = 1;
        } else if ("role_auditor".equals(roleName)) {
            roleCode = 4;
        } else {
            throw new ServiceRuntimeException("无法获取角色代码");
        }
        return roleCode;
    }

    /**
     * 获取当前用户所在班级Id
     *
     * @return
     */
    public static Integer getGradeId() {
        return getCurrentUser().getUser().getGradeId();
    }

    private static SysUserDetails getCurrentUser() {
        Object principal = getAuthentication().getPrincipal();
        if (!(principal instanceof SysUserDetails)) {
            throw new ServiceRuntimeException("登录已过期，请重新登录");
        }
        return (SysUserDetails) principal;
    }

    private static List<? extends GrantedAuthority> getAuthorities() {
        List<? extends GrantedAuthority> authorities = getAuthentication().getAuthorities()
                .stream()
                .collect(Collectors.toList());
        if (authorities.isEmpty()) {
            throw new ServiceRuntimeException("登录已过期，请重新登录");
        }
        return authorities;
    }

    private static Authentication getAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ServiceRuntimeException("登录已过期，请重新登录");
        }
        return authentication;
    }

}
