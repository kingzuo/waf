package info.yangguo.waf.controller;

import info.yangguo.waf.config.ContextHolder;
import info.yangguo.waf.dto.*;
import info.yangguo.waf.model.*;
import info.yangguo.waf.validator.ExistSequence;
import info.yangguo.waf.validator.NotExistSequence;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "api/config")
@Api(value = "api/config", description = "配置相关的接口")
public class ConfigController {
    @ApiOperation(value = "获取SecurityFilter配置")
    @ResponseBody
    @GetMapping(value = "security")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    dataType = "string", paramType = "cookie")
    })
    public ResultDto<List<SecurityConfig>> getSecurityConfigs() {
        ResultDto resultDto = new ResultDto();
        resultDto.setCode(HttpStatus.OK.value());
        List<SecurityConfig> value = ContextHolder
                .getClusterService()
                .getSecurityConfigs()
                .entrySet()
                .stream()
                .map(entry -> entry.getValue())
                .collect(Collectors.toList());
        resultDto.setValue(value);
        return resultDto;
    }

    @ApiOperation(value = "获取ResponseFilter配置")
    @ResponseBody
    @GetMapping(value = "response")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    dataType = "string", paramType = "cookie")
    })
    public ResultDto<List<ResponseConfig>> getResponseConfigs() {
        ResultDto resultDto = new ResultDto();
        resultDto.setCode(HttpStatus.OK.value());
        List<ResponseConfig> value = ContextHolder
                .getClusterService()
                .getResponseConfigs()
                .entrySet()
                .stream()
                .map(entry -> entry.getValue())
                .collect(Collectors.toList());
        resultDto.setValue(value);
        return resultDto;
    }

    @ApiOperation(value = "获取Upstream配置")
    @ResponseBody
    @GetMapping(value = "forward/http/upstream")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    dataType = "string", paramType = "cookie")
    })
    public ResultDto<List<UpstreamConfig>> getUpstreamConfigs() {
        ResultDto resultDto = new ResultDto();
        resultDto.setCode(HttpStatus.OK.value());
        List<UpstreamConfig> upstreamConfigs = ContextHolder.getClusterService().getUpstreamConfig().entrySet().stream().map(hostEntry -> {
            UpstreamConfig upstreamConfig = UpstreamConfig.builder().wafRoute(hostEntry.getKey()).config(hostEntry.getValue().getBasicConfig()).build();

            Map<String, ServerConfig> map = hostEntry.getValue().getServersMap().entrySet().stream().collect(Collectors.toMap(serverEntry -> {
                return serverEntry.getKey();
            }, entry2 -> {
                return ServerConfig.builder()
                        .ip(entry2.getValue().getIp())
                        .port(entry2.getValue().getPort())
                        .config(entry2.getValue().getConfig())
                        .build();
            }));

            hostEntry.getValue().getUnhealthilyServerConfigs().stream().forEach(server -> {
                map.get(server.getIp() + "_" + server.getPort()).setIsHealth(false);
            });
            hostEntry.getValue().getHealthilyServerConfigs().stream().forEach(server -> {
                map.get(server.getIp() + "_" + server.getPort()).setIsHealth(true);
            });

            List<ServerConfig> serverDtos = map.entrySet().stream().map(entry3 -> {
                return entry3.getValue();
            }).collect(Collectors.toList());

            upstreamConfig.setServerConfigs(serverDtos);

            return upstreamConfig;
        }).collect(Collectors.toList());
        resultDto.setValue(upstreamConfigs);
        return resultDto;
    }


    @ApiOperation(value = "获取Rewrite配置")
    @ResponseBody
    @GetMapping(value = "rewrite")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    dataType = "string", paramType = "cookie")
    })
    public ResultDto<List<RewriteConfigDto>> getRewriteConfigs() {
        ResultDto resultDto = new ResultDto();
        resultDto.setCode(HttpStatus.OK.value());
        List<RewriteConfigDto> value = ContextHolder.getClusterService().getRewriteConfigs()
                .entrySet().stream()
                .map(entry -> {
                    RewriteConfigDto rewriteConfigDto = RewriteConfigDto.builder().wafRoute(entry.getKey())
                            .isStart(entry.getValue().getIsStart()).build();
                    List<String> iterms = entry.getValue().getExtension().entrySet().stream()
                            .map(iterm -> {
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append(iterm.getKey()).append(" ");
                                List parts = (List) iterm.getValue();
                                stringBuilder.append(parts.get(0)).append(" ");
                                stringBuilder.append(parts.get(1));
                                return stringBuilder.toString();
                            }).collect(Collectors.toList());
                    rewriteConfigDto.setIterms(iterms);
                    return rewriteConfigDto;
                }).collect(Collectors.toList());
        resultDto.setValue(value);
        return resultDto;
    }


    @ApiOperation(value = "设置SecurityFilter")
    @ResponseBody
    @PutMapping(value = "security")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    dataType = "string", paramType = "cookie")
    })
    public ResultDto setRequestConfig(@RequestBody @Validated SecurityConfigDto dto) {
        ResultDto resultDto = new ResultDto();
        resultDto.setCode(HttpStatus.OK.value());
        ContextHolder.getClusterService().setSecurityConfig(Optional.of(dto.getFilterName()), Optional.of(BasicConfig.builder().isStart(dto.getIsStart()).build()));
        return resultDto;
    }

    @ApiOperation(value = "设置SecurityFilter Iterm")
    @ResponseBody
    @PutMapping(value = "security/iterm")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    dataType = "string", paramType = "cookie")
    })
    public ResultDto setRequestConfig(@RequestBody @Validated(ExistSequence.class) SecurityConfigItermDto dto) {
        ResultDto resultDto = new ResultDto();
        resultDto.setCode(HttpStatus.OK.value());

        ContextHolder.getClusterService().setSecurityConfigIterm(Optional.of(dto.getFilterName()),
                Optional.of(dto.getName()), Optional.of(BasicConfig.builder().isStart(dto.getIsStart()).extension(dto.getExtension()).build()));

        return resultDto;
    }

    @ApiOperation(value = "删除SecurityFilter Iterm")
    @ResponseBody
    @DeleteMapping(value = "security/iterm")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    dataType = "string", paramType = "cookie")
    })
    public ResultDto deleteRequestIterm(@RequestBody @Validated(NotExistSequence.class) SecurityConfigItermDto dto) {
        ResultDto resultDto = new ResultDto();
        resultDto.setCode(HttpStatus.OK.value());
        ContextHolder.getClusterService().deleteSecurityConfigIterm(Optional.of(dto.getFilterName()), Optional.of(dto.getName()));
        return resultDto;
    }

    @ApiOperation(value = "设置ResponseFilter")
    @ResponseBody
    @PutMapping(value = "response")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    dataType = "string", paramType = "cookie")
    })
    public ResultDto setResponseConfig(@RequestBody @Validated ResponseConfigDto dto) {
        ResultDto resultDto = new ResultDto();
        resultDto.setCode(HttpStatus.OK.value());
        ContextHolder.getClusterService().setResponseConfig(Optional.of(dto.getFilterName()), Optional.of(BasicConfig.builder().isStart(dto.getIsStart()).build()));
        return resultDto;
    }


    @ApiOperation(value = "设置Upstream")
    @ResponseBody
    @PutMapping(value = "forward/http/upstream")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    dataType = "string", paramType = "cookie")
    })
    public ResultDto setUpstreamConfig(@RequestBody @Validated(ExistSequence.class) UpstreamConfigDto dto) {
        ResultDto resultDto = new ResultDto();
        resultDto.setCode(HttpStatus.OK.value());
        ContextHolder.getClusterService().setUpstreamConfig(Optional.of(dto.getWafRoute()), Optional.of(BasicConfig.builder().isStart(dto.getIsStart()).build()));
        return resultDto;
    }

    @ApiOperation(value = "设置Upstream Server")
    @ResponseBody
    @PutMapping(value = "forward/http/upstream/server")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    dataType = "string", paramType = "cookie")
    })
    public ResultDto setUpstreamConfig(@RequestBody @Validated(ExistSequence.class) UpstreamServerConfigDto dto) {
        ResultDto resultDto = new ResultDto();
        resultDto.setCode(HttpStatus.OK.value());
        ContextHolder.getClusterService().setUpstreamServerConfig(Optional.of(dto.getWafRoute()),
                Optional.of(dto.getIp()),
                Optional.of(dto.getPort()),
                Optional.of(ServerBasicConfig.builder().isStart(dto.getIsStart()).weight(dto.getWeight()).build()));

        return resultDto;
    }

    @ApiOperation(value = "删除Upstream")
    @ResponseBody
    @DeleteMapping(value = "forward/http/upstream")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    dataType = "string", paramType = "cookie")
    })
    public ResultDto deleteUpstream(@RequestBody @Validated(NotExistSequence.class) UpstreamConfigDto dto) {
        ResultDto resultDto = new ResultDto();
        resultDto.setCode(HttpStatus.OK.value());
        ContextHolder.getClusterService().deleteUpstream(Optional.of(dto.getWafRoute()));
        return resultDto;
    }

    @ApiOperation(value = "删除Upstream Server")
    @ResponseBody
    @DeleteMapping(value = "forward/http/upstream/server")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    dataType = "string", paramType = "cookie")
    })
    public ResultDto deleteUpstreamServer(@RequestBody @Validated(NotExistSequence.class) UpstreamServerConfigDto dto) {
        ResultDto resultDto = new ResultDto();
        resultDto.setCode(HttpStatus.OK.value());
        ContextHolder.getClusterService().deleteUpstreamServer(Optional.of(dto.getWafRoute()), Optional.of(dto.getIp()), Optional.of(dto.getPort()));
        return resultDto;
    }


    @ApiOperation(value = "设置rewrite")
    @ResponseBody
    @PutMapping(value = "rewrite")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    dataType = "string", paramType = "cookie")
    })
    public ResultDto setRewriteConfig(@RequestBody @Validated(ExistSequence.class) RewriteConfigDto dto) {
        ResultDto resultDto = new ResultDto();
        resultDto.setCode(HttpStatus.OK.value());
        Map<String, Object> extensions = dto.getIterms().stream()
                .map(iterm -> {
                    String[] parts = iterm.split(" +");
                    return parts;
                })
                .collect(Collectors.toMap(parts -> parts[0], parts -> new String[]{parts[1], parts[2]}));
        ContextHolder.getClusterService().setRewriteConfig(Optional.of(dto.getWafRoute()), Optional.of(BasicConfig.builder().isStart(dto.getIsStart()).extension(extensions).build()));

        return resultDto;
    }

    @ApiOperation(value = "删除rewrite")
    @ResponseBody
    @DeleteMapping(value = "rewrite")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "WAFTOKEN", value = "WAFTOKEN",
                    dataType = "string", paramType = "cookie")
    })
    public ResultDto deleteRewrite(@RequestBody @Validated(NotExistSequence.class) RewriteConfigDto dto) {
        ResultDto resultDto = new ResultDto();
        resultDto.setCode(HttpStatus.OK.value());
        ContextHolder.getClusterService().deleteRewrite(Optional.of(dto.getWafRoute()));
        return resultDto;
    }
}