import React from 'react';
import Button, { ButtonGroup } from '@atlaskit/button';
import { ErrorMessage, Fieldset } from '@atlaskit/form';
import ModalDialog, { ModalTransition } from '@atlaskit/modal-dialog';
import Tooltip from '@atlaskit/tooltip';

import { colors } from 'websight-admin/theme';
import Form, { FormFooter } from 'websight-rest-atlaskit-client/Form';
import { DateTimeOffset } from 'websight-admin/DateUtils';

import PackageService from '../../services/PackageService.js';
import SchedulePackageActionRow from './SchedulePackageActionRow.js';

const ActionsErrors = (props) => {
    const { errors = [], path } = props;
    if (!errors.length) {
        return null;
    }
    return (
        <>
            {errors.map(error => error.path === path ? <ErrorMessage>{error.message}</ErrorMessage> : null)}
        </>
    )
}

const infoIconStyle = {
    display: 'inline-block',
    cursor: 'default',
    fontSize: '18px',
    verticalAlign: 'top',
    paddingLeft: '6px',
    color: colors.grey
};

const convertActionsToViewModel = (schedulePackageActions) => {
    return (schedulePackageActions || []).map(action => {
        return {
            ...action,
            schedules: (action.schedules || []).map(schedule => {
                if (schedule.at) {
                    const dateTimeOffset = new DateTimeOffset(schedule.at);
                    return {
                        at: {
                            date: dateTimeOffset.getIsoDateString(),
                            time: dateTimeOffset.getSimpleTimeString()
                        }
                    }
                }
                return schedule;
            })
        }
    });
}

const convertActionsToServerModel = (schedulePackageActions) => {
    return (schedulePackageActions || []).map(action => {
        return {
            ...action,
            schedules: (action.schedules || [])
                .map(schedule => schedule.at ? { at: `${schedule.at.date}T${schedule.at.time}:00` } : schedule)
        }
    });
}

export default class SchedulePackageActionsModal extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            isOpen: false,
            schedulePackageActions: [],
            validationErrors: []
        };

        this.open = this.open.bind(this);
        this.close = this.close.bind(this);
        this.onSubmitSuccess = this.onSubmitSuccess.bind(this);
        this.onFormSubmit = this.onFormSubmit.bind(this);
        this.addScheduleAction = this.addScheduleAction.bind(this);
        this.updateScheduleAction = this.updateScheduleAction.bind(this);
        this.addSchedule = this.addSchedule.bind(this);
        this.deleteSchedule = this.deleteSchedule.bind(this);
        this.updateSchedule = this.updateSchedule.bind(this);
    }

    open() {
        PackageService.getScheduledPackageActions(this.props.packagePath,
            ({ entity }) => {
                this.setState({
                    isOpen: true,
                    validationErrors: [],
                    serverDateTimeOffset: new DateTimeOffset(entity.currentSystemDateTime),
                    schedulePackageActions: convertActionsToViewModel(entity.scheduledPackageActions)
                });
            }
        )
    }

    close() {
        this.setState({ isOpen: false });
    }

    formActions() {
        return [
            {
                appearance: 'primary',
                type: 'submit',
                text: 'Save',
                onSubmit: this.onFormSubmit
            },
            {
                appearance: 'subtle',
                text: 'Cancel',
                onClick: this.close
            }
        ]
    }

    onFormSubmit(requestData, onSuccess, onValidationFailure, onComplete) {
        const newRequestData = {
            ...requestData,
            path: this.props.packagePath,
            actions: JSON.stringify(convertActionsToServerModel(this.state.schedulePackageActions))
        }
        const originalOnValidationFailure = onValidationFailure;
        onValidationFailure = (data) => {
            this.setState({
                validationErrors: data.entity || []
            });
            originalOnValidationFailure(data);
        }
        const originalOnComplete = onComplete;
        onComplete = (data) => {
            this.setState({
                validationErrors: []
            });
            originalOnComplete(data);
        }
        PackageService.schedulePackageActions(newRequestData, onSuccess, onValidationFailure, onComplete);
    }

    deleteSchedule(scheduleAction, indexToRemove) {
        this.setState(prevState => {
            const schedulePackageActions = prevState.schedulePackageActions;
            const scheduleActionToUpdateIndex = schedulePackageActions.indexOf(scheduleAction);
            if (scheduleActionToUpdateIndex >= 0 && schedulePackageActions[scheduleActionToUpdateIndex].schedules.length > indexToRemove) {
                if (schedulePackageActions[scheduleActionToUpdateIndex].schedules.length === 1) {
                    schedulePackageActions.splice(scheduleActionToUpdateIndex, 1)
                } else {
                    schedulePackageActions[scheduleActionToUpdateIndex].schedules.splice(indexToRemove, 1);
                }
                return ({ schedulePackageActions: schedulePackageActions });
            }
        })
    }

    updateSchedule(scheduleAction, schedule, indexOfScheduleToUpdate) {
        this.setState(prevState => {
            const schedulePackageActions = prevState.schedulePackageActions;
            const scheduleActionToUpdateIndex = schedulePackageActions.indexOf(scheduleAction);
            if (scheduleActionToUpdateIndex >= 0) {
                schedulePackageActions[scheduleActionToUpdateIndex].schedules[indexOfScheduleToUpdate] = schedule;
                return ({ schedulePackageActions: schedulePackageActions });
            }
        })
    }

    addSchedule(scheduleAction) {
        this.setState(prevState => {
            const schedulePackageActions = prevState.schedulePackageActions;
            const scheduleActionToUpdateIndex = schedulePackageActions.indexOf(scheduleAction);
            if (scheduleActionToUpdateIndex >= 0) {
                scheduleAction.schedules.push({
                    at: {
                        date: prevState.serverDateTimeOffset.getIsoDateString(),
                        time: prevState.serverDateTimeOffset.getSimpleTimeString()
                    }
                });
                schedulePackageActions[scheduleActionToUpdateIndex] = scheduleAction;
                return ({ schedulePackageActions: schedulePackageActions });
            }
        });
    }

    addScheduleAction() {
        this.setState(prevState => {
            return ({
                schedulePackageActions: [
                    ...prevState.schedulePackageActions,
                    {
                        action: 'BUILD',
                        schedules: [{
                            at: {
                                date: prevState.serverDateTimeOffset.getIsoDateString(),
                                time: prevState.serverDateTimeOffset.getSimpleTimeString()
                            }
                        }]
                    }
                ]
            })
        });
    }

    updateScheduleAction(updatedScheduleAction, index) {
        this.setState(prevState => {
            const schedulePackageActionToUpdate = prevState.schedulePackageActions[index];
            if (schedulePackageActionToUpdate) {
                schedulePackageActionToUpdate.action = updatedScheduleAction;
                return {
                    schedulePackageActions: prevState.schedulePackageActions
                };
            }
        })
    }

    setSuspended(isSuspended, index) {
        this.setState(prevState => {
            const schedulePackageAction = prevState.schedulePackageActions[index];
            if (schedulePackageAction){
                schedulePackageAction.suspended = isSuspended;
                return {
                    schedulePackageActions: prevState.schedulePackageActions
                }
            }
        })
    }

    onSubmitSuccess() {
        this.close();
        this.props.onScheduleSuccess && this.props.onScheduleSuccess()
    }

    render() {
        const { packageName } = this.props;
        const { schedulePackageActions, validationErrors, serverDateTimeOffset } = this.state;
        return (
            <ModalTransition>
                {this.state.isOpen && (
                    <ModalDialog
                        heading='Schedule actions'
                        onClose={this.close}
                    >
                        <p>Package: <b>{packageName}</b></p>
                        <p>
                            Current server time: {serverDateTimeOffset.getEnGbDateTimeOffsetString()}
                            <Tooltip tag='span' content='Scheduled actions times are defined in server time zone'>
                                <i className='material-icons' style={infoIconStyle}>info_outline</i>
                            </Tooltip>
                        </p>
                        <Form
                            onSubmit={this.onFormSubmit}
                            onSuccess={this.onSubmitSuccess}
                        >
                            {({ submitted }) => (
                                <>
                                    <Fieldset legend='Actions'>
                                        {schedulePackageActions.map((schedulePackageAction, index) => {
                                            return (
                                                <SchedulePackageActionRow
                                                    key={index}
                                                    serverDateTimeOffset={serverDateTimeOffset}
                                                    scheduleAction={schedulePackageAction}
                                                    updateScheduleAction={(updatedScheduleAction) => this.updateScheduleAction(updatedScheduleAction, index)}
                                                    setSuspendingScheduleAction={(isSuspended) => this.setSuspended(isSuspended, index)}
                                                    deleteSchedule={(indexToRemove) => this.deleteSchedule(schedulePackageAction, indexToRemove)}
                                                    addSchedule={() => this.addSchedule(schedulePackageAction)}
                                                    updateSchedule={(updatedSchedule, indexOfUpdatedSchedule) =>
                                                        this.updateSchedule(schedulePackageAction, updatedSchedule, indexOfUpdatedSchedule)}
                                                />
                                            );
                                        })}
                                        <ActionsErrors errors={validationErrors} path='actions' />
                                    </Fieldset>
                                    <Button
                                        onClick={this.addScheduleAction}
                                    >
                                        Add Action Schedule
                                    </Button>
                                    <FormFooter>
                                        <ButtonGroup>
                                            <Button
                                                appearance='primary'
                                                type='submit'
                                                isLoading={submitted}
                                            >
                                                Save
                                            </Button>
                                            <Button
                                                appearance='subtle'
                                                onClick={this.close}
                                                isDisabled={submitted}
                                            >
                                                Cancel
                                            </Button>
                                        </ButtonGroup>
                                    </FormFooter>
                                </>
                            )}
                        </Form>
                    </ModalDialog>
                )}
            </ModalTransition>
        )
    }
}
