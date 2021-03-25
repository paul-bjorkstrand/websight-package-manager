import React from 'react';
import Button from '@atlaskit/button';
import { DateTimePicker } from '@atlaskit/datetime-picker';
import { HelperMessage } from '@atlaskit/form';
import Select from '@atlaskit/select';
import Textfield from '@atlaskit/textfield';
import styled from 'styled-components';

import { colors } from 'websight-admin/theme';
import { LOCALE } from 'websight-admin/DateUtils';

const ScheduleValueContainer = styled.div`
    display: flex;
    padding-bottom: 10px;
    align-items: flex-start;
`;

const ScheduleValueTypeContainer = styled.div`
    margin: 0px 15px 0px;
    flex: 1 1 70px;
`;

const ScheduleValueFieldContainer = styled.div`
    flex: 9 1 200px;
`;

const RemoveIconContainer = styled.i`
    font-size: 20px;
    margin-top: 6px;
`;

const removeScheduleButtonStyle = {
    backgroundColor: colors.white,
    padding: 0,
    display: 'inline'
};

const expressionGeneratorHelperStyle = {
    textDecoration: 'none',
    cursor: 'pointer'
}

const newSchedule = (newScheduleType, serverDateTimeOffset) => newScheduleType === 'at' ?
    {
        at: {
            date: serverDateTimeOffset.getIsoDateString(),
            time: serverDateTimeOffset.getSimpleTimeString()
        }
    }
    :
    { cron: '' }

const getScheduleValueField = (schedule, serverDateTimeOffset, onUpdate) => {
    return (<ScheduleValueFieldContainer>
        {schedule.at ?
            <DateTimePicker
                timeIsEditable
                spacing='compact'
                locale={LOCALE}
                datePickerProps={{
                    value: schedule.at.date,
                    onChange: (newDate) => onUpdate({
                        at: {
                            date: newDate,
                            time: schedule.at.time
                        }
                    })
                }}
                timePickerProps={{
                    value: schedule.at.time,
                    onChange: (newTime) => onUpdate({
                        at: {
                            date: schedule.at.date,
                            time: newTime
                        }
                    })
                }}
            /> :
            <>
                <Textfield
                    autocomplete='off'
                    placeholder='Example: 0 0 0 * * ?'
                    isCompact={true}
                    defaultValue={schedule.cron}
                    onChange={event => onUpdate({ cron: event.target.value })}
                />
                <HelperMessage>
                    <a href='https://sojinantony01.github.io/react-cron-generator/'
                        style={expressionGeneratorHelperStyle}
                        target='_blank'
                        rel='noreferrer'>
                            Use expression generator
                    </a>
                </HelperMessage>
            </>
        }
    </ScheduleValueFieldContainer>)
}

const SCHEDULE_TYPES_OPTIONS = [
    {
        value: 'at',
        label: 'at'
    },
    {
        value: 'cron',
        label: 'cron'
    }
]

const ScheduleValueRow = (props) => {
    const { schedule } = props;
    if (!schedule) {
        return null;
    }
    return (
        <ScheduleValueContainer>
            <ScheduleValueTypeContainer>
                <Select
                    className='single-select'
                    classNamePrefix='react-select'
                    spacing='compact'
                    isRequired
                    options={SCHEDULE_TYPES_OPTIONS}
                    menuPortalTarget={document.body}
                    styles={{
                        menuPortal: base => ({
                            ...base,
                            zIndex: 9999
                        })
                    }}
                    onChange={newScheduleType => props.update(newSchedule(newScheduleType.value, props.serverDateTimeOffset))}
                    value={schedule.at ? SCHEDULE_TYPES_OPTIONS[0] : SCHEDULE_TYPES_OPTIONS[1]}
                />
            </ScheduleValueTypeContainer>
            {getScheduleValueField(schedule, props.serverDateTimeOffset, props.update)}
            <Button
                style={removeScheduleButtonStyle}
                appearance='subtle'
                spacing='compact'
                onClick={props.delete}
                title='Delete schedule'>
                <RemoveIconContainer className='material-icons'>delete</RemoveIconContainer>
            </Button>
        </ScheduleValueContainer>
    )
}

export default ScheduleValueRow;
