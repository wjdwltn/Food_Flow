package com.wltn.foodflow.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

@Slf4j
public class CustomSpringELParser {

    private CustomSpringELParser() {}

    public static Object getDynamicValue(String[] parameterNames, Object[] args, String key) {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext();

        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]); // 1) 메서드의 파라미터 이름과 값을 EvaluationContext에 설정한다.
        }

        try {
            return parser.parseExpression(key).getValue(context, Object.class); // 2)SpEL 표현식(key)을 통해 파싱한다.
        } catch (Exception e) {
            log.error("SpEL evaluation error: key={}, error={}", key, e.getMessage());
            throw e;
        }
    }
}
