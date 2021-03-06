package com.centit.framework.system.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.centit.framework.common.*;
import com.centit.framework.components.CodeRepositoryUtil;
import com.centit.framework.components.SysUnitFilterEngine;
import com.centit.framework.components.SysUserFilterEngine;
import com.centit.framework.components.impl.UserUnitMapTranslate;
import com.centit.framework.core.controller.BaseController;
import com.centit.framework.core.controller.WrapUpResponseBody;
import com.centit.framework.filter.RequestThreadLocal;
import com.centit.framework.model.basedata.*;
import com.centit.framework.security.model.CentitUserDetails;
import com.centit.support.algorithm.CollectionsOpt;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * @author codefan@sina.com
 * Date: 14-11-26
 * Time: 下午1:53
 * 返回原框架中使用CP标签的结果数据
 */
@Controller
@RequestMapping("/cp")
@Api(value="框架将所有的系统信息都缓存在内存中，这个类提供了大量的访问框架数据的接口",
    tags= "框架数据缓存接口")
public class CacheController extends BaseController {

    public String getOptId (){
        return "mainframe";
    }
    /**
     * cp标签中MAPVALUE实现，获取数据字典对应的值
     *
     * @param catalog  系统内置的类别 字符串[userCode,loginName,
     *                             unitcode,depno,rolecode,optid,optcode,optdesc,]
     *                             以及数据目录中的catalogCode变量值
     * @param key      对应的变量值 或 数据字典中的 dataCode
     * @return ResponseData
     */
    @ApiOperation(value="数据字典取值",notes="根据数据字典的类别和key获取对应的value。")
    @ApiImplicitParams({@ApiImplicitParam(
        name = "catalog", value="数据字典的类别代码",
        required=true, paramType = "path", dataType= "String"
    ),@ApiImplicitParam(
        name = "key", value="数据字典的条目代码",
        required= true, paramType = "path", dataType= "String"
    )})
    @RequestMapping(value = "/mapvalue/{catalog}/{key}", method = RequestMethod.GET)
    //@RecordOperationLog(content = "查询字典{arg0}中{arg1}的值",timing = true, appendRequest = true)
    @ResponseBody
    public ResponseData mapvalue(@PathVariable String catalog, @PathVariable String key) {
        String value = CodeRepositoryUtil.getValue(catalog, key);
        return ResponseData.makeResponseData(value);
    }

    /**
     * cp标签中MAPCODE实现，获取数据字典对应的值
     *
     * @param catalog  系统内置的类别 字符串[userCode,loginName,unitcode,depno,rolecode,optid,optcode,optdesc,]以及数据目录中的catalogCode变量值
     * @param value    对应的变量值 或 数据字典中的 dataValue
     * @return String 数据字典代码
     */
    @ApiOperation(value="数据字典取健",notes="和mapvalue相反他是根据数据字典的类别和value获取对应的key。")
    @ApiImplicitParams({@ApiImplicitParam(
        name = "catalog", value="数据字典的类别代码",
        required=true, paramType = "path", dataType= "String"
    ),@ApiImplicitParam(
        name = "value", value="数据字典的值",
        required= true, paramType = "path", dataType= "String"
    )})
    @RequestMapping(value = "/mapcode/{catalog}/{value}", method = RequestMethod.GET)
    @WrapUpResponseBody
    public String mapcode(@PathVariable String catalog, @PathVariable String value) {
        return CodeRepositoryUtil.getCode(catalog, value);
        //return ResponseData.makeResponseData(key);
    }


    /**
     * cp标签中LVB实现，获取 数据字典 key value 对
     *
     * @param catalog  系统内置的类别
     *                 字符串[userCode,loginName,unitcode,depno,
     *                 rolecode,optid,optcode,optdesc,]，
     *                 数据目录中的catalogCode变量值
     * @return Map 数据字典对应表
     */
    @ApiOperation(value="获取数据字典明细",notes="根据数据字典类别代码获取数据字典明细。")
    @ApiImplicitParam(
        name = "catalog", value="数据字典的类别代码",
        required= true, paramType = "path", dataType= "String"
    )
    @RequestMapping(value = "/lvb/{catalog}", method = RequestMethod.GET)
    @WrapUpResponseBody
    public Map<String,String> lvb(@PathVariable String catalog) {
        Map<String,String> keyValueMap = CodeRepositoryUtil.getLabelValueMap(catalog);
        if(keyValueMap==null || keyValueMap.isEmpty()){
            throw new ObjectException(catalog,ObjectException.DATA_NOT_FOUND_EXCEPTION,"找不到对应的数据字典内容。");
        }
        return keyValueMap;
    }

    /**
     * cp标签中MAPEXPRESSION实现
     * 把表达式中的字典代码都 转换为 数据字典值，其他的字符 位置不变，
     *
     * @param catalog    数据字典代码
     * @param expression 表达式
     */
    @ApiOperation(value = "将字典代码转字典值", notes = "根据字典代码转成对应的字典值，其他的字符位置不变")
    @ApiImplicitParams({@ApiImplicitParam(
        name = "catalog", value="数据字典的类别代码",
        required=true, paramType = "path", dataType= "String"
    ),@ApiImplicitParam(
        name = "expression", value="表达式",
        required= true, paramType = "path", dataType= "String"
    )})
    @RequestMapping(value = "/mapexpression/{catalog}/{expression}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseData mapexpression(@PathVariable String catalog, String expression) {
        String s = CodeRepositoryUtil.transExpression(catalog, expression);
        return ResponseData.makeResponseData(s);
    }

    /**
     * cp标签中MAPSTATE实现，获得数据字典条目的状态
     *
     * @param catalog  系统内置的类别 字符串[userCode,loginName,unitcode,rolecode,]以及数据目录中的catalogCode变量值
     * @param key      对应的变量值 或 数据字典中的 dataCode
     */
    @ApiOperation(value = "获得数据字典条目的状态", notes = "根据字典代码和条目获取条目的状态")
    @ApiImplicitParams({@ApiImplicitParam(
        name = "catalog", value="数据字典的类别代码",
        required=true, paramType = "path", dataType= "String"
    ),@ApiImplicitParam(
        name = "key", value="数据字典的条目代码",
        required= true, paramType = "path", dataType= "String"
    )})
    @RequestMapping(value = "/mapstate/{catalog}/{key}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseData mapstate(@PathVariable String catalog, @PathVariable String key) {
        String s = CodeRepositoryUtil.getItemState(catalog, key);
        return ResponseData.makeResponseData(s);
    }

    /**
     * cp标签中SUBUNITS实现
     * 获取机构下面的所有下级机构，并且排序
     *
     * @param unitCode 机构代码
     * @param unitType 机构类别
     */
    @ApiOperation(value = "获取机构下面的所有下级机构", notes = "获取机构下面的所有下级机构，并且排序")
    @ApiImplicitParams({@ApiImplicitParam(
        name = "unitCode", value="机构的代码",
        required=true, paramType = "path", dataType= "String"
    ),@ApiImplicitParam(
        name = "unitType", value="机构类别",
        required= true, paramType = "path", dataType= "String"
    )})
    @RequestMapping(value = "/subunits/{unitCode}/{unitType}", method = RequestMethod.GET)
    @WrapUpResponseBody
    public ResponseData subunits(@PathVariable String unitCode, @PathVariable String unitType) {
        List<IUnitInfo> listObjects = CodeRepositoryUtil.getSortedSubUnits(unitCode, unitType);
        return ResponseData.makeResponseData(listObjects);
    }

    /**
     * cp标签中ALLUNITS实现
     * 根据状态获取所有机构信息，
     *
     * @param state    A表示所有状态
     */
    @ApiOperation(value = "根据状态获取所有机构信息", notes = "根据状态获取所有机构信息")
    @ApiImplicitParam(
        name = "state", value="状态，A表示所有状态",
        required= true, paramType = "path", dataType= "String"
    )
    @RequestMapping(value = "/allunits/{state}", method = RequestMethod.GET)
    @WrapUpResponseBody
    public ResponseData allunits(@PathVariable String state) {
        List<IUnitInfo> listObjects = CodeRepositoryUtil.getAllUnits(state);
//        JsonResultUtils.writeSingleDataJson(listObjects, response);
        return ResponseData.makeResponseData(listObjects);
    }

    /**
     * 根据用户编码获取用户信息
     *
     * @param userCode 用户编码
     */
    @ApiOperation(value = "根据用户编码获取用户信息", notes = "根据用户编码获取用户详细信息")
    @ApiImplicitParam(
        name = "userCode", value="用户编码",
        required= true, paramType = "path", dataType= "String"
    )
    @RequestMapping(value = "/userinfo/{userCode}", method = RequestMethod.GET)
    @WrapUpResponseBody
    public ResponseData getUserInfo(@PathVariable String userCode) {
        return ResponseData.makeResponseData(CodeRepositoryUtil.getUserInfoByCode(userCode));
    }

    /**
     * 根据机构编码获取机构信息
     *
     * @param unitCode 机构代码
     */
    @ApiOperation(value = "根据机构代码获取机构信息", notes = "根据机构代码获取机构详细信息")
    @ApiImplicitParam(
        name = "unitCode", value="机构代码",
        required= true, paramType = "path", dataType= "String"
    )
    @RequestMapping(value = "/unitinfo/{unitCode}", method = RequestMethod.GET)
    @WrapUpResponseBody
    public ResponseData getUintInfo(@PathVariable String unitCode) {
        return ResponseData.makeResponseData(CodeRepositoryUtil.getUnitInfoByCode(unitCode));
    }

    /**
     * 根据机构代码获取父机构
     *
     * @param unitCode 机构代码
     */
    @ApiOperation(value = "根据机构代码获取父机构", notes = "根据机构代码获取父机构详细信息")
    @ApiImplicitParam(
        name = "unitCode", value="机构代码",
        required= true, paramType = "path", dataType= "String"
    )
    @RequestMapping(value = "/parentunit/{unitCode}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseData getParentUintInfo(@PathVariable String unitCode) {
        IUnitInfo ui = CodeRepositoryUtil.getUnitInfoByCode(unitCode);
        if(ui!=null){
           return ResponseData.makeResponseData(ui.getParentUnit());
        }else {
           return ResponseData.makeErrorMessage("没有代码为: "+ unitCode+" 的机构！");
        }
    }

    /**
     * 根据机构代码获取父机构路径
     *
     * @param unitCode 机构代码
     */
    @ApiOperation(value = "根据机构代码获取父机构路径", notes = "根据机构代码获取父机构的机构路径")
    @ApiImplicitParam(
        name = "unitCode", value="机构代码",
        required= true, paramType = "path", dataType= "String"
    )
    @RequestMapping(value = "/parentpath/{unitCode}", method = RequestMethod.GET)
    @WrapUpResponseBody
    public ResponseData getParentUintPath(@PathVariable String unitCode) {
        IUnitInfo ui = CodeRepositoryUtil.getUnitInfoByCode(unitCode);
        if(ui!=null){
            List<IUnitInfo> parentUnits = new ArrayList<>();
            while(true) {
                if(StringUtils.isBlank(ui.getParentUnit())){
                    break;
                }
                IUnitInfo parentUnit = CodeRepositoryUtil.getUnitInfoByCode(ui.getParentUnit());
                if(parentUnit==null){
                    break;
                }
                parentUnits.add(parentUnit);
                ui = parentUnit;
            }
            return ResponseData.makeResponseData(parentUnits);
        }else {
            return ResponseData.makeErrorMessage("没有代码为: "+ unitCode+" 的机构！");
        }
    }
    /**
     * cp标签中RECURSEUNITS实现
     * 获得已知机构 下级的所有有效机构并返回map，包括下级机构的下级机构
     *  @param parentUnit 父级机构代码
     * @param response   HttpServletResponse
     */
    @ApiOperation(value = "获得已知机构 下级的所有有效机构", notes = "获得已知机构 下级的所有有效机构并返回map，包括下级机构的下级机构")
    @ApiImplicitParam(
        name = "parentUnit", value="父级机构代码",
        required= true, paramType = "path", dataType= "String"
    )
    @RequestMapping(value = "/recurseunits/{parentUnit}", method = RequestMethod.GET)
    @WrapUpResponseBody
    public ResponseSingleData recurseUnits(@PathVariable String parentUnit, HttpServletResponse response) {
        Map<String, IUnitInfo> objects = CodeRepositoryUtil.getUnitMapBuyParaentRecurse(parentUnit);
        return ResponseData.makeResponseData(objects);
    }

    /**
     * CP标签中DICTIONARY实现
     * 获取数据字典
     *
     * @param catalog  数据目类别码
     * @param extraCode  扩展代码
     * @param request  HttpServletRequest
     *
     */
    @ApiOperation(value = "获取数据字典", notes = "根据类别编码和扩展编码获取数据字典")
    @ApiImplicitParams({@ApiImplicitParam(
        name = "catalog", value="字典类别代码",
        required= true, paramType = "path", dataType= "String"
    ),@ApiImplicitParam(
        name = "extraCode", value="扩展代码",
        paramType = "query", dataType= "String"
    )})
    @RequestMapping(value = "/dictionary/{catalog}", method = RequestMethod.GET)
    @WrapUpResponseBody
    public ResponseData dictionary(@PathVariable String catalog, String extraCode,
            HttpServletRequest request) {
        List<? extends IDataDictionary> listObjects = CodeRepositoryUtil.getDictionary(catalog);

        String lang = WebOptUtils.getCurrentLang(request);
        JSONArray dictJson = new JSONArray();
        for(IDataDictionary dict : listObjects){
            // 级联或者树形数据字典明细查询
            if (StringUtils.isNotBlank(extraCode) && !extraCode.equals(dict.getExtraCode()))
                continue;
            JSONObject obj = (JSONObject)JSON.toJSON(dict);
            obj.put("dataValue", dict.getLocalDataValue(lang));
            dictJson.add(obj);
        }
        return ResponseData.makeResponseData(dictJson);
    }

    /**
     * CP标签中DICTIONARY_D实现
     * 根据字典类别编码获取数据字典 ，忽略 tag 为 'D'的条目 【delete】
     *
     * @param catalog  数据目录代码
     * @param request  HttpServletRequest
     */
    @ApiOperation(value = "根据字典类别编码获取数据字典", notes = "根据字典类别编码获取数据字典 ，忽略 tag 为 'D'的条目 【delete】")
    @ApiImplicitParam(
        name = "catalog", value="字典类别编码",
        required= true, paramType = "path", dataType= "String"
    )
    @RequestMapping(value = "/dictionaryd/{catalog}", method = RequestMethod.GET)
    @WrapUpResponseBody
    public ResponseData dictionaryd(@PathVariable String catalog,
            HttpServletRequest request) {
        List<? extends IDataDictionary> listObjects = CodeRepositoryUtil.getDictionaryIgnoreD(catalog);

        String lang = WebOptUtils.getCurrentLang(request);
        JSONArray dictJson = new JSONArray();
        for(IDataDictionary dict : listObjects){
            JSONObject obj = (JSONObject)JSON.toJSON(dict);
            obj.put("dataValue", dict.getLocalDataValue(lang));
            dictJson.add(obj);
        }
        return ResponseData.makeResponseData(dictJson);
    }

    /**
     * CP标签中UNITUSER实现
     * 获取一个机构下面的所有用户，并且根据排序号排序
     *
     * @param unitCode 机构代码
     */
    @ApiOperation(value = "获取一个机构下面的所有用户", notes = "获取一个机构下面的所有用户，并且根据排序号排序")
    @ApiImplicitParam(
        name = "unitCode", value="机构代码",
        required= true, paramType = "path", dataType= "String"
    )
    @RequestMapping(value = "/unituser/{unitCode}", method = RequestMethod.GET)
    @WrapUpResponseBody
    public ResponseData unituser(@PathVariable String unitCode) {
        List<IUserInfo> listObjects = CodeRepositoryUtil.getSortedUnitUsers(unitCode);
        return ResponseData.makeResponseData(listObjects);
    }

    /**
     * CP标签中ALLUSER实现
     * 获取所有符合状态标记的用户，
     *
     * @param state    用户状态， A 表示所有状态
     */
    @ApiOperation(value = "获取所有符合状态标记的用户", notes = "根据状态获取所有用户")
    @ApiImplicitParam(
        name = "state", value="用户状态 A 表示所有状态",
        required= true, paramType = "path", dataType= "String"
    )
    @RequestMapping(value = "/alluser/{state}", method = RequestMethod.GET)
    @WrapUpResponseBody
    public ResponseData alluser(@PathVariable String state) {
        List<IUserInfo> listObjects = CodeRepositoryUtil.getAllUsers(state);
        return ResponseData.makeResponseData(listObjects);
    }

    /**
     * CP标签中UNITSLIST实现
     * 获取所有下级机构，
     *
     * @param unitcode 机构代码
     */
    @ApiOperation(value = "获取所有下级机构", notes = "根据机构代码获取所有下级机构")
    @ApiImplicitParam(
        name = "unitcode", value="机构代码",
        required= true, paramType = "path", dataType= "String"
    )
    @RequestMapping(value = "/subunits/{unitcode}", method = RequestMethod.GET)
    @WrapUpResponseBody
    public ResponseData getSubUnits(@PathVariable String unitcode) {
        List<IUnitInfo> listObjects = CodeRepositoryUtil.getSubUnits(unitcode);
//        JsonResultUtils.writeSingleDataJson(listObjects, response);
        return ResponseData.makeResponseData(listObjects);
    }

    /**
     * 获取机构的下级机构，并按照树形排列
     *
     * @param unitcode 机构代码
     * @param response HttpServletResponse
     */
    @ApiOperation(value = "获取所有下级机构树形排列", notes = "根据机构代码获取所有下级机构，并按照树形排列")
    @ApiImplicitParam(
        name = "unitcode", value="机构代码",
        required= true, paramType = "path", dataType= "String"
    )
    @WrapUpResponseBody
    @RequestMapping(value = "/allsubunits/{unitcode}", method = RequestMethod.GET)
    public ResponseData getAllSubUnits(@PathVariable String unitcode, HttpServletResponse response) {
        List<IUnitInfo> listObjects = CodeRepositoryUtil.getAllSubUnits(unitcode);
        return ResponseData.makeResponseData(listObjects);

    }

    /**
     * 实现 机构表达式过滤
     * 获取所有符合状态标记的用户，
     *
     * @param unitfilter 机构代码
     * @param request HttpServletRequest
     * @return IUnitInfo 机构列表
     */
    @ApiOperation(value = "根据机构表达式获取符合条件的机构",
        notes = "根据机构表达式获取符合条件的结构，系统通过机构规则引擎计算;表达式完整式 D()DT()DL()")
    @ApiImplicitParam(
        name = "unitfilter", value="机构表达式，示例：D('D111' - 2)",
        required= true, paramType = "path", dataType= "String"
    )
    //@RecordOperationLog(content = "查询机构表达式为{unitfilter}的机构")
    // @ParamName("unitfilter")
    @RequestMapping(value = "/unitfilter/{unitfilter}", method = RequestMethod.GET)
    @WrapUpResponseBody
    public List<IUnitInfo> unitfilter(@PathVariable String unitfilter,
                           //@RequestBody Map<String,Object> varMap,
                           HttpServletRequest request) {

        Map<String, Set<String>> unitParams = null;
        CentitUserDetails ud = WebOptUtils.getLoginUser(request);
        if(ud!=null){
            //String usercode = ud.getUserCode();
            String userUnit = ud.getCurrentUnitCode();
            if(userUnit!=null){
                unitParams = new HashMap<>();
                unitParams.put("U", CollectionsOpt.createHashSet(userUnit));
            }
        }
        Set<String> units =  SysUnitFilterEngine.calcSystemUnitsByExp(
            StringEscapeUtils.unescapeHtml4(unitfilter),
            unitParams, new UserUnitMapTranslate());
        /*List<IUnitInfo> listObjects = new ArrayList<>();
        for(String uc : units){
            listObjects.add( CodeRepositoryUtil.getUnitInfoByCode(uc) );
        }*/
        List<IUnitInfo> retUntis = CodeRepositoryUtil.getUnitInfosByCodes(units);
        CollectionsOpt.sortAsTree(retUntis,
            ( p,  c) -> StringUtils.equals(p.getUnitCode(),c.getParentUnit()) );
        return retUntis;
    }

    /**
     * 实现 机构表达式过滤
     * 获取所有符合状态标记的用户，
     *
     * @param userfilter 用户代码
     * @param request request
     * @return IUserInfo 用户列表
     */
    @ApiOperation(value = "根据用户表达式计算符合条件的用户",
        notes = "根据用户表达式计算符合条件的用户; 表达式完整式 D()DT()DL()GW()XZ()R()UT()UL()U()")
    @ApiImplicitParam(
        name = "userfilter", value="用户表达式，示例：D(all)DT('D')xz('部门经理')",
        required= true, paramType = "path", dataType= "String"
    )
    @RequestMapping(value = "/userfilter/{userfilter}", method = RequestMethod.GET)
    @WrapUpResponseBody
    public  List<IUserInfo> userfilter(@PathVariable String userfilter,
                           //@RequestBody Map<String,Object> varMap,
                           HttpServletRequest request) {
        Map<String, Set<String>> unitParams = null;
        Map<String, Set<String>> userParams = null;
        CentitUserDetails ud = WebOptUtils.getLoginUser(request);
        if(ud!=null){
            String userCode = ud.getUserCode();
            if(userCode!=null){
                unitParams = new HashMap<>();
                unitParams.put("O", CollectionsOpt.createHashSet(userCode));
            }
            String userUnit = ud.getCurrentUnitCode();
            if(userUnit!=null){
                unitParams = new HashMap<>();
                unitParams.put("U", CollectionsOpt.createHashSet(userUnit));
            }
        }
        Set<String> users =  SysUserFilterEngine.calcSystemOperators(
                StringEscapeUtils.unescapeHtml4(userfilter),
                unitParams,userParams,null, new UserUnitMapTranslate());
        /*List<IUserInfo> listObjects = new ArrayList<>();
        for(String uc : users){
            listObjects.add( CodeRepositoryUtil.getUserInfoByCode(uc));
        }*/
        return CodeRepositoryUtil.getUserInfosByCodes(users);
    }


    private JSONArray makeMenuFuncsJson(List<IOptInfo> menuFunsByUser) {
        return ViewDataTransform.makeTreeViewJson(menuFunsByUser,
                ViewDataTransform.createStringHashMap("id", "optId",
                        "optId", "optId",
                        "pid", "preOptId",
                        "text", "optName",
                        "optName", "optName",
                        "url", "optRoute",
                        "icon", "icon",
                        "children", "children",
                        "isInToolbar", "isInToolbar",
                        "state", "state"
                ), (jsonObject, obj) ->
                        jsonObject.put("external", !("D".equals(obj.getPageType()))));
    }

    /**
     * CP标签中OPTINFO实现
     * 按类别获取 业务定义信息
     *    S:实施业务, O:普通业务, W:流程业务, I:项目业务  , M:菜单   A: 为所有
     * @param optType  业务类别
     */
    @ApiOperation(value = "按类别获取业务菜单信息", notes = "按类别获取业务菜单信息")
    @ApiImplicitParam(
        name = "optType", value="业务类别",
        required= true, paramType = "path", dataType= "String"
    )
    @RequestMapping(value = "/optinfo/{optType}", method = RequestMethod.GET)
    @WrapUpResponseBody
    public ResponseData optinfoByTypeAsMenu(@PathVariable String optType) {
        List<IOptInfo> listObjects = CodeRepositoryUtil.getOptinfoList(optType);
        return ResponseData.makeResponseData(makeMenuFuncsJson(listObjects));
    }

    /**
     * CP标签中OPTDEF实现
     * 获得一个业务下面的操作定义
     *
     * @param optID    系统业务代码
     */
    @ApiOperation(value = "获得业务下面的操作定义", notes = "获得一个业务下面的操作定义")
    @ApiImplicitParam(
        name = "optID", value="系统业务代码",
        required= true, paramType = "path", dataType= "String"
    )
    @RequestMapping(value = "/optdef/{optID}", method = RequestMethod.GET)
    @WrapUpResponseBody
    public ResponseData optdef(@PathVariable String optID) {
        List<? extends IOptMethod> listObjects = CodeRepositoryUtil.getOptMethodByOptID(optID);
        return ResponseData.makeResponseData(listObjects);
    }

    /**
     *
     * 根据角色类别获取角色
     *
     * @param roleType 角色类别
     */
    @ApiOperation(value = "根据角色类别获取角色", notes = "根据角色类别获取角色")
    @ApiImplicitParam(
        name = "roleType", value="角色类别",
        required= true, paramType = "path", dataType= "String"
    )
    @RequestMapping(value = "/roleinfo/{roleType}", method = RequestMethod.GET)
    @WrapUpResponseBody
    public ResponseData roleinfo(@PathVariable String roleType) {
        List<IRoleInfo> listObjects = CodeRepositoryUtil.getRoleinfoListByType(roleType);
        return ResponseData.makeResponseData(listObjects);
    }

    /**
     * CP标签中 SYS_VALUE 实现
     *  获取系统设置的值
     * @param request  HttpServletRequest
     * @param response HttpServletResponse
     */
    @ApiOperation(value = "获取系统设置的值", notes = "根据参数代码获取系统设置的值")
    @ApiImplicitParam(
        name = "paramCode", value="参数代码",
        required= true,paramType = "path", dataType= "String"
    )
    @RequestMapping(value = "/sysconfig/{paramCode}", method = RequestMethod.GET)
    @WrapUpResponseBody
    public ResponseData getSysConfigValue(HttpServletRequest request, HttpServletResponse response) {
        String uri = request.getRequestURI();
        String paramCode = uri.substring(uri.lastIndexOf('/')+1);
        String pv =  CodeRepositoryUtil.getSysConfigValue(paramCode);
        return ResponseData.makeResponseData(pv);
    }

    /**
     * CP标签中 SYS_VALUE 实现
     * 系统设置的参数的前缀
     * @param request HttpServletRequest
     **/
    @ApiOperation(value = "根据参数前缀获取系统设置", notes = "根据参数前缀获取系统设置")
    @ApiImplicitParam(
        name = "prefix", value="前缀",
        required= true,paramType = "path", dataType= "String"
    )
    @RequestMapping(value = "/sysconfigbyprefix/{prefix}", method = RequestMethod.GET)
    @WrapUpResponseBody
    public ResponseData getSysConfigByPrefix(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String prefix = uri.substring(uri.lastIndexOf('/')+1);
        Map<String, Object> pv =  CodeRepositoryUtil.getSysConfigByPrefix(prefix);
        return ResponseData.makeResponseData(pv);
    }

    /**
     * 获取用户信息
     */
    @ApiOperation(value = "获取当前登录用户详情", notes = "获取当前登录用户详情，包括其组织机构、权限信息、用户设置等等")
    @RequestMapping(value = "/userdetails", method = RequestMethod.GET)
    @WrapUpResponseBody
    public ResponseData getUserDetails() {
        CentitUserDetails userDetails = WebOptUtils.getLoginUser(RequestThreadLocal.getHttpThreadWrapper()
                .getRequest());
        return ResponseData.makeResponseData(userDetails);
    }

    /**
     * CP标签中USERSETTING实现
     * 获取用户当前设置值
     *
     * @param paramCode 用户设置的参数
     */
    @ApiOperation(value = "获取用户当前设置值", notes = "获取用户当前设置值")
    @ApiImplicitParam(
        name = "paramCode", value="用户设置的参数",
        required= true,paramType = "path", dataType= "String"
    )
    @RequestMapping(value = "/usersetting/{paramCode}", method = RequestMethod.GET)
    @WrapUpResponseBody
    public ResponseData getUserSettingValue(@PathVariable String paramCode) {
        String pv =  CodeRepositoryUtil.getUserSettingValue(paramCode);
        return ResponseData.makeResponseData(pv);
    }

    /**
     * 获取用户所有设置
     */
    @ApiOperation(value = "获取用户所有设置", notes = "获取用户所有设置")
    @RequestMapping(value = "/usersettings", method = RequestMethod.GET)
    @WrapUpResponseBody
    public ResponseData getUserAllSettings() {
        return ResponseData.makeResponseData(CodeRepositoryUtil.getUserAllSettings());
    }

    /**
     * 验证当前用户是否有某个操作方法的权限
     *
     * @param optId 系统业务代码
     * @param method 权限代码
     */
    @ApiOperation(value = "验证当前用户是否有某个操作方法的权限", notes = "验证当前用户是否有某个操作方法的权限")
    @ApiImplicitParams({
        @ApiImplicitParam(
            name = "optId", value="系统业务代码",
            required= true,paramType = "path", dataType= "String"),
        @ApiImplicitParam(
        name = "method", value="操作方法",
        required= true,paramType = "path", dataType= "String")
    })
    @RequestMapping(value = "/checkuserpower/{optId}/{method}", method = { RequestMethod.GET })
    @WrapUpResponseBody
    public ResponseData checkUserOptPower(@PathVariable String optId,@PathVariable String method) {
        boolean s = CodeRepositoryUtil.checkUserOptPower(optId,method);
        return ResponseData.makeResponseData(s);
    }

    /**
     * 获取用户所有的 操作方法
     * 返回一个map，key为optid+‘-’+method value 为 'T'
     */
    @ApiOperation(value = "获取用户所有的 操作方法", notes = "获取用户所有的 操作方法")
    @RequestMapping(value = "/userallpowers", method = { RequestMethod.GET })
    @WrapUpResponseBody
    public ResponseData getUserAllPowers() {
        return ResponseData.makeResponseData(CodeRepositoryUtil.getUserAllOptPowers());
    }

}
