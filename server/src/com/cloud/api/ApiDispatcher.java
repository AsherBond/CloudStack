/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.api;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd.CommandType;
import com.cloud.async.AsyncCommandQueued;
import com.cloud.exception.AccountLimitException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.server.ManagementServer;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.utils.DateUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * A class that dispatches API commands to the appropriate manager for execution.
 */
public class ApiDispatcher {
    private static final Logger s_logger = Logger.getLogger(ApiDispatcher.class.getName());

    ComponentLocator _locator = null;

    // singleton class
    private static ApiDispatcher s_instance = new ApiDispatcher();

    public static ApiDispatcher getInstance() {
        return s_instance;
    }

    private ApiDispatcher() {
        _locator = ComponentLocator.getLocator(ManagementServer.Name);
    }

    public void dispatchCreateCmd(BaseAsyncCreateCmd cmd, Map<String, String> params) {
        setupParameters(cmd, params);

        try {
            cmd.callCreate();
        } catch (Throwable t) {
            if (t instanceof  InvalidParameterValueException || t instanceof IllegalArgumentException) {
                s_logger.info(t);
                throw new ServerApiException(BaseCmd.PARAM_ERROR, t.getMessage());
            }else if (t instanceof PermissionDeniedException) {
                s_logger.info(t);
                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, t.getMessage());
            }else if (t instanceof AccountLimitException) {
                s_logger.info(t);
                throw new ServerApiException(BaseCmd.ACCOUNT_RESOURCE_LIMIT_ERROR, t.getMessage());
            }else if (t instanceof InsufficientCapacityException) {
                s_logger.info(t);
                throw new ServerApiException(BaseCmd.INSUFFICIENT_CAPACITY_ERROR, t.getMessage());
            }else if (t instanceof ResourceAllocationException) {
                s_logger.info(t);
                throw new ServerApiException(BaseCmd.RESOURCE_ALLOCATION_ERROR, t.getMessage());
            }else if (t instanceof ResourceUnavailableException) {
                s_logger.warn(t);
                throw new ServerApiException(BaseCmd.RESOURCE_UNAVAILABLE_ERROR, t.getMessage());
            }else if (t instanceof ServerApiException) {
                s_logger.warn(t);
                throw new ServerApiException(((ServerApiException) t).getErrorCode(), ((ServerApiException) t).getDescription());
            }else if (t instanceof AsyncCommandQueued) {
                throw (AsyncCommandQueued)t;
            }else {
                s_logger.warn(t);
                s_logger.error("Exception while executing " + cmd.getClass().getSimpleName() + ":", t);
                if (UserContext.current().getAccount() == null || UserContext.current().getAccount().getType() == Account.ACCOUNT_TYPE_ADMIN)
                    throw new ServerApiException(BaseCmd.INTERNAL_ERROR, t.getMessage());
                else
                    throw new ServerApiException(BaseCmd.INTERNAL_ERROR, BaseCmd.USER_ERROR_MESSAGE);
            }
        }
    }

    public void dispatch(BaseCmd cmd, Map<String, String> params) {
        setupParameters(cmd, params);
        try {
            cmd.execute();
        } catch (Throwable t) {
            if (t instanceof  InvalidParameterValueException || t instanceof IllegalArgumentException) {
                s_logger.info(t);
                throw new ServerApiException(BaseCmd.PARAM_ERROR, t.getMessage());
            }else if (t instanceof PermissionDeniedException) {
                s_logger.info(t);
                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, t.getMessage());
            }else if (t instanceof AccountLimitException) {
                s_logger.info(t);
                throw new ServerApiException(BaseCmd.ACCOUNT_RESOURCE_LIMIT_ERROR, t.getMessage());
            }else if (t instanceof InsufficientCapacityException) {
                s_logger.info(t);
                throw new ServerApiException(BaseCmd.INSUFFICIENT_CAPACITY_ERROR, t.getMessage());
            }else if (t instanceof ResourceAllocationException) {
                s_logger.warn(t);
                throw new ServerApiException(BaseCmd.RESOURCE_ALLOCATION_ERROR, t.getMessage());
            }else if (t instanceof ResourceUnavailableException) {
                s_logger.warn(t);
                throw new ServerApiException(BaseCmd.RESOURCE_UNAVAILABLE_ERROR, t.getMessage());
            }else if (t instanceof ServerApiException) {
                s_logger.warn(t);
                throw new ServerApiException(((ServerApiException) t).getErrorCode(), ((ServerApiException) t).getDescription());
            } else if (t instanceof AsyncCommandQueued) {
                throw (AsyncCommandQueued)t;
            }else {
                s_logger.warn(t);
                s_logger.error("Exception while executing " + cmd.getClass().getSimpleName() + ":", t);
                if (UserContext.current().getAccount() == null || UserContext.current().getAccount().getType() == Account.ACCOUNT_TYPE_ADMIN)
                    throw new ServerApiException(BaseCmd.INTERNAL_ERROR, t.getMessage());
                else
                    throw new ServerApiException(BaseCmd.INTERNAL_ERROR, BaseCmd.USER_ERROR_MESSAGE);
            }
        }
    }

    public static void setupParameters(BaseCmd cmd, Map<String, String> params){
        Map<String, Object> unpackedParams = cmd.unpackParams(params);
        
        Field[] fields = cmd.getClass().getDeclaredFields();
        Class<?> superClass = cmd.getClass().getSuperclass();
        while (BaseCmd.class.isAssignableFrom(superClass)) {
            Field[] superClassFields = superClass.getDeclaredFields();
            if (superClassFields != null) {
                Field[] tmpFields = new Field[fields.length + superClassFields.length];
                System.arraycopy(fields, 0, tmpFields, 0, fields.length);
                System.arraycopy(superClassFields, 0, tmpFields, fields.length, superClassFields.length);
                fields = tmpFields;
            }
            superClass = superClass.getSuperclass();
        }

        for (Field field : fields) {
            Parameter parameterAnnotation = field.getAnnotation(Parameter.class);
            if ((parameterAnnotation == null) || !parameterAnnotation.expose()) {
                continue;
            }

            Object paramObj = unpackedParams.get(parameterAnnotation.name());
            if (paramObj == null) {
                if (parameterAnnotation.required()) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to execute API command " + cmd.getName() + " due to missing parameter " + parameterAnnotation.name());
                }
                continue;
            }

            // marshall the parameter into the correct type and set the field value
            try {
                setFieldValue(field, cmd, paramObj, parameterAnnotation);
            } catch (IllegalArgumentException argEx) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unable to execute API command " + cmd.getName() + " due to invalid value " + paramObj + " for parameter " + parameterAnnotation.name());
                }
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to execute API command " + cmd.getName() + " due to invalid value " + paramObj + " for parameter " + parameterAnnotation.name());
            } catch (ParseException parseEx) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Invalid date parameter " + paramObj + " passed to command " + cmd.getName());
                }
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to parse date " + paramObj + " for command " + cmd.getName() + ", please pass dates in the format yyyy-MM-dd");
            } catch (CloudRuntimeException cloudEx) {
                // FIXME: Better error message? This only happens if the API command is not executable, which typically means
                // there was
                // and IllegalAccessException setting one of the parameters.
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Internal error executing API command " + cmd.getName());
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void setFieldValue(Field field, BaseCmd cmdObj, Object paramObj, Parameter annotation) throws IllegalArgumentException, ParseException {
        try {
            field.setAccessible(true);
            CommandType fieldType = annotation.type();
            switch (fieldType) {
            case BOOLEAN:
                field.set(cmdObj, Boolean.valueOf(paramObj.toString()));
                break;
            case DATE:
                DateFormat format = BaseCmd.INPUT_FORMAT;
                synchronized (format) {
                    field.set(cmdObj, format.parse(paramObj.toString()));
                }
                break;
            case FLOAT:
                field.set(cmdObj, Float.valueOf(paramObj.toString()));
                break;
            case INTEGER:
                field.set(cmdObj, Integer.valueOf(paramObj.toString()));
                break;
            case LIST:
                List listParam = new ArrayList();
                StringTokenizer st = new StringTokenizer(paramObj.toString(), ",");
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    CommandType listType = annotation.collectionType();
                    switch (listType) {
                    case INTEGER:
                        listParam.add(Integer.valueOf(token));
                        break;
                    case LONG:
                        listParam.add(Long.valueOf(token));
                        break;
                    case STRING:
                        listParam.add(token);
                        break;
                    }
                }
                field.set(cmdObj, listParam);
                break;
            case LONG:
                field.set(cmdObj, Long.valueOf(paramObj.toString()));
                break;
            case STRING:
                field.set(cmdObj, paramObj.toString());
                break;
            case TZDATE:
                field.set(cmdObj, DateUtil.parseTZDateString(paramObj.toString()));
                break;
            case MAP:
            default:
                field.set(cmdObj, paramObj);
                break;
            }
        } catch (IllegalAccessException ex) {
            s_logger.error("Error initializing command " + cmdObj.getName() + ", field " + field.getName() + " is not accessible.");
            throw new CloudRuntimeException("Internal error initializing parameters for command " + cmdObj.getName() + " [field " + field.getName() + " is not accessible]");
        }
    }
}
