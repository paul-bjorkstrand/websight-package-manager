import React from 'react';

import { colors } from 'websight-admin/theme'

export const Separator = () => {
    return (
        <>
            <br/>
            <hr style={{ borderWidth: '1px 0px 0px 0px', color: colors.white }}/>
            <br/>
        </>
    )
}

const Header = (props) => {
    const { title, path, date } = props;
    return (
        <>
            <span><b>{title}:</b> {path}</span><br/>
            <span>{date.toString()}</span><br/>
            <br/>
        </>
    )
}

const Footer = (props) => {
    return (
        <>
            <br/>
            <ExecutionTime {...props}/>
        </>
    )
}

const ExecutionTime = (props) => {
    const { title, timeInMillis } = props;
    return (
        <>
            <span>{title} in {timeInMillis}ms.</span><br/>
        </>
    )
}

const Filter = (props) => {
    const spaces = '           '; // no tabs in html
    const pathStartColumnIndex = 10;
    const { mode, root, rules } = props.filter;
    return (
        <>
            <span>{mode.toLowerCase()}:{spaces.substr(0, pathStartColumnIndex - mode.length) + root}</span>
            <br/>
            {(rules || []).map(rule => (
                <>
                    <Rule {...rule} />
                    <br/>
                </>
            ))}
        </>
    );
}

const Rule = (props) => {
    const { include, pattern } = props;
    const content = '  ' + (include ? 'include' : 'exclude') + ': ' + pattern;
    return <span style={{ color: include ? colors.veryDarkGreen : colors.red }}>{content}</span>;
}

export const Filters = (props) => {
    const header = <Header title='Show Filters' path={props.path} date={props.date}/>;
    const footer = <Footer title='Filters shown' timeInMillis={0}/>;

    if ((props.filters || []).length === 0) {
        return (
            <>
                {header}
                <span>Package {props.name} has no filters</span><br/>
                {footer}
            </>
        )
    }

    const filters = [];
    let previousFilterHadRules = true;
    (props.filters || []).forEach(filter => {
        const hasRules = (filter.rules || []).length > 0;
        if (hasRules || previousFilterHadRules) {
            filters.push(<br/>);
        }
        filters.push(<Filter filter={filter}/>);
        previousFilterHadRules = hasRules;
    });
    return (
        <>
            {header}
            <span>Package {props.name} filters:</span><br/>
            {filters}
            {footer}
        </>
    )
}

const ShowFullLogHeader = (props) => {
    const endpoint = '/apps/websight-package-manager-service/bin/package.log';
    const fullUri = `${endpoint}?path=${props.path}`;
    return (
        <>
            <a
                href={fullUri}
                target='_blank'
                rel='noreferrer'
            >
                Show Full Log
            </a><br/>
            <br/>
        </>
    )
}

const formattedLineStarts = [
    'Build Package:',
    'Building package',
    'Install Package: ',
    'Installing content',
    'Package Coverage Preview',
    'Test Install Package:',
    'Uninstall Package:',
    'A ',
    'D ',
    'U ',
    '-'
];

const formattedLines = [
    'Collecting import information...',
    'Creating snapshot for',
    'Dry Run: Skipping node types installation (might lead to errors).',
    'Dump package coverage',
    'Importing content...',
    'Installing content (dry run)',
    'Installing privileges...',
    'Installing node types...',
    'Package import simulation finished.',
    'Package imported.',
    'reverting approx',
    'saving approx',
    'Simulating content import...',
    'Unable to revert package content',
    'Uninstalling content',
    'Uninstalling package'
];

const checkIfLineStartShouldBeFormatted = (line) => {
    return formattedLineStarts.find(lineStart => line.startsWith(lineStart));
}

const checkIfWholeLineShouldBeFormatted = (line) => {
    return formattedLines.find(lineStart => line.startsWith(lineStart));
}

const checkIfIsFailedMessage = (line) => {
    return line.match('^Package .+ failed.$');
}

export const PackageLogs = (props) => {
    const result = (props.logs || []).map((line, index) => {
        if (checkIfWholeLineShouldBeFormatted(line)) {
            return <span key={index}><b>{line}</b></span>
        }
        const lineStartToBeFormatted = checkIfLineStartShouldBeFormatted(line);
        if (lineStartToBeFormatted) {
            return <span key={index}><b>{lineStartToBeFormatted}</b>{line.substr(lineStartToBeFormatted.length)}</span>
        }
        if (checkIfIsFailedMessage(line)) {
            return <span key={index} style={{ color: colors.red }}>{line}</span>
        }
        return <div key={index}>{line}</div>;
    });
    return (
        <>
            <ShowFullLogHeader path={props.path}/>
            {result}
        </>
    )
}
