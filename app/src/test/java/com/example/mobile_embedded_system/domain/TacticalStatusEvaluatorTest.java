package com.example.mobile_embedded_system.domain;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Тестирование порогов физиологического статуса бойца (ТЗ §5.1, §6.7).
 */
public class TacticalStatusEvaluatorTest {

    @Test
    public void testNominalValuesReturnOk() {
        TacticalStatusEvaluator.Status status = TacticalStatusEvaluator.evaluate(72, 36.6);
        assertEquals(TacticalStatusEvaluator.Status.OK, status);
    }

    @Test
    public void testWarningThresholds() {
        // Проверка порога ЧСС (> 95)
        assertEquals(TacticalStatusEvaluator.Status.OK, TacticalStatusEvaluator.evaluate(95, 36.6));
        assertEquals(TacticalStatusEvaluator.Status.WARNING, TacticalStatusEvaluator.evaluate(96, 36.6));

        // Проверка порога температуры (> 37.2)
        assertEquals(TacticalStatusEvaluator.Status.OK, TacticalStatusEvaluator.evaluate(75, 37.2));
        assertEquals(TacticalStatusEvaluator.Status.WARNING, TacticalStatusEvaluator.evaluate(75, 37.3));
    }

    @Test
    public void testCriticalThresholds() {
        // Брадикардия (< 45)
        assertEquals(TacticalStatusEvaluator.Status.OK, TacticalStatusEvaluator.evaluate(45, 36.6));
        assertEquals(TacticalStatusEvaluator.Status.CRITICAL, TacticalStatusEvaluator.evaluate(44, 36.6));

        // Тахикардия (> 120)
        assertEquals(TacticalStatusEvaluator.Status.WARNING, TacticalStatusEvaluator.evaluate(120, 36.6));
        assertEquals(TacticalStatusEvaluator.Status.CRITICAL, TacticalStatusEvaluator.evaluate(121, 36.6));

        // Гипертермия (> 38.5)
        assertEquals(TacticalStatusEvaluator.Status.WARNING, TacticalStatusEvaluator.evaluate(75, 38.5));
        assertEquals(TacticalStatusEvaluator.Status.CRITICAL, TacticalStatusEvaluator.evaluate(75, 38.6));
    }

    @Test
    public void testCriticalOverridesWarning() {
        // Если температура в WARNING, но ЧСС в CRITICAL — общий статус должен быть CRITICAL
        assertEquals(TacticalStatusEvaluator.Status.CRITICAL, TacticalStatusEvaluator.evaluate(130, 37.4));
    }

    /** Верхние границы WARNING: пульс 95 и температура 37.2 ещё номинальные, 120 / 38.5 ещё не CRITICAL. */
    @Test
    public void bothBoundariesAtUpperWarningLimitReturnWarning() {
        assertEquals(TacticalStatusEvaluator.Status.WARNING, TacticalStatusEvaluator.evaluate(120, 38.5));
    }

    @Test
    public void bothBoundariesAtNominalLimitReturnOk() {
        assertEquals(TacticalStatusEvaluator.Status.OK, TacticalStatusEvaluator.evaluate(95, 37.2));
        assertEquals(TacticalStatusEvaluator.Status.OK, TacticalStatusEvaluator.evaluate(45, 37.2));
    }

    @Test
    public void warningTemperatureWithWarningPulseStaysWarning() {
        assertEquals(TacticalStatusEvaluator.Status.WARNING, TacticalStatusEvaluator.evaluate(96, 37.3));
    }

    @Test
    public void criticalTemperatureOverridesWarningPulse() {
        assertEquals(TacticalStatusEvaluator.Status.CRITICAL, TacticalStatusEvaluator.evaluate(96, 38.6));
    }

    @Test
    public void zeroPulseIsCritical() {
        assertEquals(TacticalStatusEvaluator.Status.CRITICAL, TacticalStatusEvaluator.evaluate(0, 36.6));
    }

    @Test
    public void extremeValuesAreCritical() {
        assertEquals(TacticalStatusEvaluator.Status.CRITICAL, TacticalStatusEvaluator.evaluate(Integer.MAX_VALUE, 36.6));
        assertEquals(TacticalStatusEvaluator.Status.CRITICAL, TacticalStatusEvaluator.evaluate(-1, 36.6));
        assertEquals(TacticalStatusEvaluator.Status.CRITICAL, TacticalStatusEvaluator.evaluate(70, 45.0));
    }

    @Test
    public void lowTemperatureAloneDoesNotRaiseStatus() {
        // Пороги заданы только сверху: переохлаждение этим классом не оценивается.
        assertEquals(TacticalStatusEvaluator.Status.OK, TacticalStatusEvaluator.evaluate(70, 30.0));
    }
}
