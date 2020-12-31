import React from 'react';
import Button from '@atlaskit/button';
import Select from '@atlaskit/select';
import styled from 'styled-components';

import { colors } from 'websight-admin/theme';

import ScheduleValueRow from './ScheduleValueRow.js';

const SCHEDULE_ACTION_SELECT_INITIAL_WIDTH = 100;

const ScheduleActionContainer = styled.div`
    margin: 10px 0 10px 24px;
    padding-bottom: 4px
    border-bottom: 1px solid ${colors.veryLightGrey};
`;

const ScheduleBasicInfoContainer = styled.div`
    display: flex;
    align-items: flex-start;
`;

const ScheduleActionSelectContainer = styled.div`
    flex: 0 0 ${SCHEDULE_ACTION_SELECT_INITIAL_WIDTH}px;
`;

const SchedulesFirstValueContainer = styled.div`
    width: 100%;
`

const SchedulesNextValuesContainer = styled.div`
    margin-left: ${SCHEDULE_ACTION_SELECT_INITIAL_WIDTH}px;
`;

const SchedulesOptions = styled.div`
    margin-left: ${SCHEDULE_ACTION_SELECT_INITIAL_WIDTH + 15}px;
    display: flex;
    align-items: center;
`;

const ActionSuspendedInfo = styled.div`
    display: flex;
`;

const ActionSuspendInfoText = styled.p`
    padding: 0;
    font-size: smaller;
    margin: 4px 0px 0px 4px;
`;

const ActionSuspendedIcon = styled.i`
    font-size: 21px;
    color: ${colors.yellow};
`;

const SCHEDULE_ACTION_TYPES = [
    { value: 'BUILD', label: 'Build' },
    { value: 'INSTALL', label: 'Install' },
    { value: 'UNINSTALL', label: 'Uninstall' },
    { value: 'DELETE', label: 'Delete' }
];

const SchedulePackageActionRow = (props) => {
    const { scheduleAction } = props;
    const schedules = ((scheduleAction && scheduleAction.schedules) || []);
    return (
        <ScheduleActionContainer>
            <ScheduleBasicInfoContainer>
                <ScheduleActionSelectContainer>
                    <Select
                        className='single-select'
                        classNamePrefix='react-select'
                        spacing='compact'
                        isDisabled={!!scheduleAction.id}
                        isRequired
                        options={SCHEDULE_ACTION_TYPES}
                        placeholder='Select Action'
                        menuPortalTarget={document.body}
                        styles={{
                            container: base => ({
                                ...base,
                                paddingBottom: '10px'
                            }),
                            menuPortal: base => ({
                                ...base,
                                zIndex: 9999
                            })
                        }}
                        value={SCHEDULE_ACTION_TYPES.find(({ value }) => value === scheduleAction.action)}
                        onChange={newScheduleAction => props.updateScheduleAction(newScheduleAction.value)}
                    />
                </ScheduleActionSelectContainer>
                {(schedules && schedules.length > 0) &&
                <SchedulesFirstValueContainer>
                    <ScheduleValueRow
                        update={(updatedSchedule) => props.updateSchedule(updatedSchedule, 0)}
                        delete={() => props.deleteSchedule(0)}
                        schedule={schedules[0]}
                        serverDateTimeOffset={props.serverDateTimeOffset} />
                </SchedulesFirstValueContainer>
                }
            </ScheduleBasicInfoContainer>
            {(schedules && schedules.length > 1) && (
                <SchedulesNextValuesContainer>
                    {schedules.map((schedule, index) => {
                        if (index !== 0) {
                            return (
                                <ScheduleValueRow
                                    key={index}
                                    update={(updatedSchedule) => props.updateSchedule(updatedSchedule, index)}
                                    delete={() => props.deleteSchedule(index)}
                                    serverDateTimeOffset={props.serverDateTimeOffset}
                                    schedule={schedule} />
                            )
                        }
                    })}
                </SchedulesNextValuesContainer>
            )}
            <SchedulesOptions>
                <Button
                    onClick={props.addSchedule}
                >
                    Add Schedule
                </Button>
                <Button
                    style={{ margin: '0 15px' }}
                    onClick={() => props.setSuspendingScheduleAction(!scheduleAction.suspended)}
                >
                    {scheduleAction.suspended ? 'Resume all' : 'Suspend all'}
                </Button>
                {scheduleAction.suspended && (
                    <ActionSuspendedInfo>
                        <ActionSuspendedIcon
                            className='material-icons'>
                            warning
                        </ActionSuspendedIcon>
                        <ActionSuspendInfoText>Action suspended</ActionSuspendInfoText>
                    </ActionSuspendedInfo>
                )}
            </SchedulesOptions>
        </ScheduleActionContainer>
    )
}

export default SchedulePackageActionRow;
